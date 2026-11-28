package me.bluejay.regraded.regrade;

import me.bluejay.regraded.bits.BitApplicator;
import me.bluejay.regraded.bits.ChiselsAndBitsApplicator;
import me.bluejay.regraded.core.Points;
import me.bluejay.regraded.core.SurveyPoint;
import me.bluejay.regraded.core.TinSurface;
import net.minecraft.core.BlockPos;
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
 * Mojang mappings / 26.1.2.
 *
 * Coordinate convention matches LevelHeaded and the white markers:
 * world X = easting, world Z = -northing.
 */
public final class Regrade {
    private static final int BITS = 16;
    private static final int CUT_ABOVE = 8;
    private static double lastMin = Double.NaN;
    private static double lastMax = Double.NaN;
    private static int lastCutBlocks = 0;
    private static int lastClearedBits = 0;

    private Regrade() {}

    public static int column(ServerLevel world, int x, int z) {
        List<SurveyPoint> pts = Points.list().points();
        if (pts.isEmpty()) return 0;
        double elev = TinSurface.elevationAt(pts, x + 0.5, -(z + 0.5));
        if (Double.isNaN(elev)) return 0;
        if (Double.isNaN(lastMin) || elev < lastMin) lastMin = elev;
        if (Double.isNaN(lastMax) || elev > lastMax) lastMax = elev;

        int surfaceY = (int) Math.floor(elev);
        BlockPos surface = new BlockPos(x, surfaceY, z);
        clearBlockers(world, surface);
        BlockState mat = materialBeneath(world, surface);
        BitApplicator app = BitApplicator.current();
        int written = 0;

        // Cut whole blocks above the surface.
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dy = 1; dy <= CUT_ABOVE; dy++) {
            BlockPos above = new BlockPos(x, surfaceY + dy, z);
            if (world.getBlockState(above).isAir()) continue;
            world.setBlock(above, air, 3);
            lastCutBlocks++;
            written++;
        }

        // One mutator for the whole block, committed before the next column.
        app.beginBatch(world, surface);
        try {
            // Convert the surface block to a chiseled block so clear works on it.
            app.setBit(world, surface, 0, 0, 0, mat);

            // Fill up to the surface bit, then clear every bit above it.
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
                    if (localY != surfaceY) continue;
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
        return written;
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
    }

    public static String elevationRange() {
        if (Double.isNaN(lastMin)) return "";
        return String.format(" elev %.2f..%.2f cut %d blocks, %d bits", lastMin, lastMax, lastCutBlocks, lastClearedBits);
    }

    public static String failure() {
        return ChiselsAndBitsApplicator.lastFailure();
    }
}
