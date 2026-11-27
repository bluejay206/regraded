package me.bluejay.regraded.regrade;

import me.bluejay.regraded.bits.BitApplicator;
import me.bluejay.regraded.bits.ChiselsAndBitsApplicator;
import me.bluejay.regraded.core.Points;
import me.bluejay.regraded.core.SurveyPoint;
import me.bluejay.regraded.core.TinSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Applies a TIN surface to a block column or a chunk using the bit applicator.
 * Writes one bit layer at the interpolated elevation so elevation changes show.
 * Mojang mappings / 26.1.2.
 *
 * Coordinate convention matches LevelHeaded and the white markers:
 * world X = easting, world Z = -northing.
 */
public final class Regrade {
    private static final int BITS = 16;
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
        BlockState mat = ChiselsAndBitsApplicator.defaultMaterial();
        BitApplicator app = BitApplicator.current();
        int written = 0;

        for (int dy = -1; dy <= 1; dy++) {
            BlockPos origin = new BlockPos(x, surfaceY + dy, z);
            double blockBase = origin.getY();
            for (int bx = 0; bx < BITS; bx++) {
                for (int bz = 0; bz < BITS; bz++) {
                    double easting = x + (bx + 0.5) / BITS;
                    double northing = -(z + (bz + 0.5) / BITS);
                    double localElev = TinSurface.elevationAt(pts, easting, northing);
                    if (Double.isNaN(localElev)) continue;
                    double bitY = (localElev - blockBase) * BITS;
                    int by = (int) Math.floor(bitY);
                    if (by < 0 || by >= BITS) continue;
                    if (app.setBit(world, origin, bx, by, bz, mat)) written++;
                }
            }
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
