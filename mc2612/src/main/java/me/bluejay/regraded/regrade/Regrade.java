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
 * Fill: dirt bits at and below the interpolated elevation.
 * Cut: clear bits above the surface in the surface block, and set blocks
 * above the surface to air.
 * Mojang mappings / 26.1.2.
 *
 * Coordinate convention matches LevelHeaded and the white markers:
 * world X = easting, world Z = -northing.
 */
public final class Regrade {
    private static final int BITS = 16;
    private static final int CUT_ABOVE = 4;
    private static double lastMin = Double.NaN;
    private static double lastMax = Double.NaN;

    private Regrade() {}

    public static int column(ServerLevel world, int x, int z) {
        List<SurveyPoint> pts = Points.list().points();
        if (pts.isEmpty()) return 0;
        double elev = TinSurface.elevationAt(pts, x + 0.5, -(z + 0.5));
        if (Double.isNaN(elev)) return 0;
        if (Double.isNaN(lastMin) || elev < lastMin) lastMin = elev;
        if (Double.isNaN(lastMax) || elev > lastMax) lastMax = elev;

        int surfaceY = (int) Math.floor(elev);
        int surfaceBit = (int) Math.floor((elev - surfaceY) * BITS);
        if (surfaceBit < 0) surfaceBit = 0;
        if (surfaceBit >= BITS) surfaceBit = BITS - 1;

        BlockState mat = ChiselsAndBitsApplicator.defaultMaterial();
        BitApplicator app = BitApplicator.current();
        int written = 0;

        // Fill: dirt bits in the surface block, up to the surface bit.
        BlockPos surface = new BlockPos(x, surfaceY, z);
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
                // Cut: clear bits above the surface in this block.
                for (int by = localBit + 1; by < BITS; by++) {
                    if (app.clearBit(world, surface, bx, by, bz)) written++;
                }
            }
        }

        // Cut: remove whole blocks above the surface.
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dy = 1; dy <= CUT_ABOVE; dy++) {
            BlockPos above = new BlockPos(x, surfaceY + dy, z);
            BlockState existing = world.getBlockState(above);
            if (existing.isAir()) continue;
            world.setBlock(above, air, 3);
            written++;
        }
        return written;
    }

    public static int block(ServerLevel world, BlockPos origin) {
        lastMin = Double.NaN;
        lastMax = Double.NaN;
        return column(world, origin.getX(), origin.getZ());
    }

    public static int chunk(ServerLevel world, ChunkPos chunk, int unusedSurfaceY) {
        lastMin = Double.NaN;
        lastMax = Double.NaN;
        int written = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                written += column(world, (chunk.x() << 4) + x, (chunk.z() << 4) + z);
            }
        }
        return written;
    }

    public static String elevationRange() {
        if (Double.isNaN(lastMin)) return "";
        return String.format(" elev %.2f..%.2f", lastMin, lastMax);
    }

    public static String failure() {
        return ChiselsAndBitsApplicator.lastFailure();
    }
}
