package me.bluejay.regraded.client;

import me.bluejay.regraded.core.Points;
import me.bluejay.regraded.core.SurveyPoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/**
 * White dust particles at each loaded survey point. Toggle with Ctrl-Z.
 * Coordinate convention matches LevelHeaded: easting = X, northing = -Z.
 */
public final class PointMarkers {
    private static boolean visible;

    private PointMarkers() {}

    public static boolean visible() { return visible; }

    public static void toggle() {
        visible = !visible;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.inGameHud != null) {
            int count = Points.size();
            String msg = visible ? "ReGraded markers on (" + count + " points)" : "ReGraded markers off";
            client.inGameHud.setOverlayMessage(Text.literal(msg), false);
        }
    }

    public static void tick(MinecraftClient client) {
        if (!visible || client.world == null || client.player == null) return;
        Vec3d eye = client.player.getEyePos();
        for (SurveyPoint p : Points.list().points()) {
            double x = p.easting();
            double y = p.elevation();
            double z = -p.northing();
            if (eye.squaredDistanceTo(x, y, z) > 256 * 256) continue;
            client.world.addParticle(
                    new DustParticleEffect(new Vector3f(1.0f, 1.0f, 1.0f), 1.2f),
                    x, y + 0.1, z, 0.0, 0.0, 0.0);
        }
    }
}
