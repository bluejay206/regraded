package me.bluejay.regraded.regrade;

import me.bluejay.regraded.bits.BitApplicator;
import me.bluejay.regraded.bits.ChiselsAndBitsApplicator;
import me.bluejay.regraded.core.Points;
import me.bluejay.regraded.core.SurveyPoint;
import me.bluejay.regraded.core.TinSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Applies a TIN surface to a block column or a chunk using the bit applicator.
 * Fill: bits of the block beneath, up to the interpolated elevation.
 * Cut: set blocks above the surface to air, convert the surface block, and
 * clear the bits above the surface elevation.
 * Plants, flowers and other non-solid blocks at and above the surface are
 * removed first so they cannot block the chisel.
 *
 * Continuity: if the TIN crosses into the block above (air), a solid material
 * block is placed there and queued for regrade. If it crosses into the block
 * below, that block is queued. This keeps the plane contiguous across block
 * boundaries without holes or peaks.
 *
 * Mojang mappings / 26.1.2.
 *
 * Coordinate convention matches LevelHeaded and the white markers:
 * world X = easting, world Z = -northing.
 */
public final class Regrade {
    private static final int BITS = 16;
    private static final int CUT_ABOVE = 8;
    private static final Identifier CHISELED = Identifier.fromNamespaceAndPath("chiselsandbits", "chiseled");

    private static double lastMin = Double.NaN;
    private static double lastMax = Double.NaN;
    private static int lastCutBlocks = 0;
    private static int lastClearedBits = 0;
    private static int lastRemoved = 0;

    private Regrade() {}

    /**
     * Process one column. {@code targetY} is the block Y to chisel;
     * pass null to derive it from the center elevation.
     * Returns the number of bits written. Adjacent blocks that the TIN
     * crosses into are placed (if air) and queued via RegradeQueue.
     */
    public static int column(ServerLevel world, int x, int z, Integer targetY) {
        List<SurveyPoint> pts = Points.list().points();
        if (pts.isEmpty()) return 0;
        double elev = TinSurface.elevationAt(pts, x + 0.5, -(z + 0.5));
        if (Double.isNaN(elev)) return 0;
        if (Double.isNaN(lastMin) || elev < lastMin) lastMin = elev;
        if (Double.isNaN(lastMax) || elev > lastMax) lastMax = elev;

        int surfaceY = targetY != null ? targetY : (int) Math.floor(elev);
        BlockPos surface = new BlockPos(x, surfaceY, z);
        clearBlockers(world, surface);
        BlockState mat = materialBeneath(world, surface);
        BitApplicator app = BitApplicator.current();
        int written = 0;
        boolean needsAbove = false;
        boolean needsBelow = false;

        // Cut whole blocks above the surface (only for the primary pass).
        if (targetY == null) {
            BlockState air = Blocks.AIR.defaultBlockState();
            for (int dy = 1; dy <= CUT_ABOVE; dy++) {
                BlockPos above = new BlockPos(x, surfaceY + dy, z);
                if (world.getBlockState(above).isAir()) continue;
                world.setBlock(above, air, 3);
                lastCutBlocks++;
                written++;
            }
        }

        app.beginBatch(world, surface);
        try {
            app.setBit(world, surface, 0, 0, 0, mat);

            for (int bx = 0; bx < BITS; bx++) {
                for (int bz = 0; bz < BITS; bz++) {
                    double easting = x + (bx + 0.5) / BITS;
                    double northing = -(z + (bz + 0.5) / BITS);
                    double localElev = TinSurface.elevationAt(pts, easting, northing);
                    if (Double.isNaN(localElev)) continue;
                    int localY = (int) Math.floor(localElev);
                    int localBit = (int) Math.floor((localElev - localY) * BITS);
                    if (localBit < 0) localBit = 0;
                    if (localBit >= BITS) localBit = BITS - 1;

                    if (localY > surfaceY) {
                        needsAbove = true;
                        continue;
                    }
                    if (localY < surfaceY) {
                        needsBelow = true;
                        continue;
                    }

                    for (int by = 0; by <= localBit; by++) {
                        if (app.setBit(world, surface, bx, by, bz, mat)) written++;
                    }
                    for (int by = localBit + 1; by < BITS; by++) {
                        if (app.clearBit(world, surface, bx, by, bz)) {
                            lastClearedBits++;
                            written++;
                        }
                    }
                }
            }
        } finally {
            app.endBatch();
        }

        // Continuity: place and queue adjacent blocks the TIN crosses into.
        if (needsAbove) {
            BlockPos above = surface.above();
            if (world.getBlockState(above).isAir()) {
                world.setBlock(above, mat, 3);
            }
            RegradeQueue.enqueueColumnAt(world, x, z, surfaceY + 1);
        }
        if (needsBelow) {
            RegradeQueue.enqueueColumnAt(world, x, z, surfaceY - 1);
        }

        return written;
    }

    public static int column(ServerLevel world, int x, int z) {
        return column(world, x, z, null);
    }

    /** Remove plants, flowers and other non-solid blocks that block chiseling. */
    private static void clearBlockers(ServerLevel world, BlockPos surface) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dy = 0; dy <= CUT_ABOVE; dy++) {
            BlockPos pos = surface.above(dy);
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) continue;
            if (!state.isSolidRender()) {
                world.setBlock(pos, air, 3);
                lastCutBlocks++;
            }
        }
    }

    /** Block state to use for bits: the solid block at or just below the surface. */
    private static BlockState materialBeneath(ServerLevel world, BlockPos surface) {
        BlockState here = world.getBlockState(surface);
        if (!here.isAir() && here.isSolidRender() && here.getFluidState().isEmpty()) return here;
        for (int dy = 1; dy <= 4; dy++) {
            BlockState below = world.getBlockState(surface.below(dy));
            if (!below.isAir() && below.isSolidRender() && below.getFluidState().isEmpty()) return below;
        }
        return ChiselsAndBitsApplicator.defaultMaterial();
    }

    /**
     * Remove every chiselsandbits:chiseled block in the chunk.
     * Returns the number of blocks cleared.
     */
    public static int removeChunk(ServerLevel world, ChunkPos chunk) {
        lastRemoved = 0;
        BlockState air = Blocks.AIR.defaultBlockState();
        int minY = world.getMinY();
        int maxY = world.getMaxY();
        int baseX = chunk.x() << 4;
        int baseZ = chunk.z() << 4;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    BlockPos pos = new BlockPos(baseX + x, y, baseZ + z);
                    BlockState state = world.getBlockState(pos);
                    if (isChiseled(state)) {
                        world.setBlock(pos, air, 3);
                        lastRemoved++;
                    }
                }
            }
        }
        return lastRemoved;
    }

    private static boolean isChiseled(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null && CHISELED.equals(id);
    }

    public static int block(ServerLevel world, BlockPos origin) {
        resetStats();
        return column(world, origin.getX(), origin.getZ());
    }

    public static int chunk(ServerLevel world, ChunkPos chunk, int unusedSurfaceY) {
        resetStats();
        int written = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                written += column(world, (chunk.x() << 4) + x, (chunk.z() << 4) + z);
            }
        }
        return written;
    }

    static void resetStats() {
        lastMin = Double.NaN;
        lastMax = Double.NaN;
        lastCutBlocks = 0;
        lastClearedBits = 0;
        lastRemoved = 0;
    }

    public static String elevationRange() {
        if (Double.isNaN(lastMin)) return "";
        return String.format(" elev %.2f..%.2f cut %d blocks, %d bits", lastMin, lastMax, lastCutBlocks, lastClearedBits);
    }

    public static int lastRemoved() { return lastRemoved; }

    public static String failure() {
        return ChiselsAndBitsApplicator.lastFailure();
    }
}
