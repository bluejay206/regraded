package me.bluejay.regraded.client;

import me.bluejay.regraded.core.Points;
import me.bluejay.regraded.core.SurveyPoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/**
 * White particle column at each loaded survey point. Toggle with Ctrl-Z.
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

    public static void registerRender() {
        // WorldRenderEvents box removed. Particles only.
    }

    /** White particle column. */
    public static void tick(MinecraftClient client) {
        if (!visible || client.world == null || client.player == null) return;
        Vec3d eye = client.player.getEyePos();
        for (SurveyPoint p : Points.list().points()) {
            double x = p.easting();
            double y = p.elevation();
            double z = -p.northing();
            if (eye.squaredDistanceTo(x, y, z) > 256 * 256) continue;
            for (int i = 0; i < 4; i++) {
                client.world.addParticle(
                        ParticleTypes.END_ROD,
                        true,
                        x, y + 0.2 + i * 0.4, z,
                        0.0, 0.02, 0.0);
            }
        }
    }
}
