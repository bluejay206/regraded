package me.bluejay.regraded.regrade;

import me.bluejay.regraded.bits.BitApplicator;
import me.bluejay.regraded.bits.ChiselsAndBitsApplicator;
import me.bluejay.regraded.core.Points;
import me.bluejay.regraded.core.SurveyPoint;
import me.bluejay.regraded.core.TinSurface;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.List;

/**
 * Applies a TIN surface to a block column or a chunk using the bit applicator.
 * Coordinate convention: SurveyPoint.easting = world X, northing = world Z.
 */
public final class Regrade {
    private static final int BITS = 16;

    private Regrade() {}

    public static int block(ServerWorld world, BlockPos origin) {
        List<SurveyPoint> pts = Points.list().points();
        if (pts.isEmpty()) return 0;
        BlockState mat = ChiselsAndBitsApplicator.defaultMaterial();
        BitApplicator app = BitApplicator.current();
        int written = 0;
        for (int bx = 0; bx < BITS; bx++) {
            for (int bz = 0; bz < BITS; bz++) {
                double wx = origin.getX() + (bx + 0.5) / BITS;
                double wz = origin.getZ() + (bz + 0.5) / BITS;
                double elev = TinSurface.elevationAt(pts, wx, wz);
                if (Double.isNaN(elev)) continue;
                int by = (int) Math.round((elev - origin.getY()) * BITS);
                by = Math.max(0, Math.min(BITS - 1, by));
                if (app.setBit(world, origin, bx, by, bz, mat)) written++;
            }
        }
        return written;
    }

    public static int chunk(ServerWorld world, ChunkPos chunk) {
        int written = 0;
        int baseY = world.getBottomY();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                BlockPos pos = new BlockPos(chunk.getStartX() + x, baseY, chunk.getStartZ() + z);
                written += block(world, pos);
            }
        }
        return written;
    }
}
