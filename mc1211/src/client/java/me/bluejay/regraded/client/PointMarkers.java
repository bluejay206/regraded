package me.bluejay.regraded.client;

import me.bluejay.regraded.core.PointList;
import me.bluejay.regraded.core.SurveyPoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/**
 * White particle markers (Yarn). Toggled by Ctrl-Z.
 */
public final class PointMarkers {
    private static boolean visible;
    private static PointList points = new PointList();

    private PointMarkers() {}

    public static boolean visible() { return visible; }

    public static void toggle() { visible = !visible; }

    public static void setPoints(PointList list) {
        points = list == null ? new PointList() : list;
    }

    public static void tick(MinecraftClient client) {
        if (!visible || client.world == null || client.player == null) return;
        Vec3d eye = client.player.getEyePos();
        for (SurveyPoint p : points.points()) {
            double x = p.easting();
            double y = p.elevation();
            double z = -p.northing();
            if (eye.squaredDistanceTo(x, y, z) > 256 * 256) continue;
            client.world.addParticle(
                    new DustParticleEffect(new Vector3f(1f, 1f, 1f), 1.2f),
                    x, y + 0.1, z,
                    0.0, 0.0, 0.0
            );
        }
    }
}
