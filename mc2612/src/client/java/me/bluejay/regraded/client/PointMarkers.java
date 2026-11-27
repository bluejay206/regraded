package me.bluejay.regraded.client;

import me.bluejay.regraded.core.Points;
import me.bluejay.regraded.core.SurveyPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * White particle columns at imported survey points.
 * Coordinates: x = easting, y = elevation, z = -northing (same as LevelHeaded).
 */
public final class PointMarkers {
    private static boolean visible;

    private PointMarkers() {}

    public static boolean visible() { return visible; }

    public static void toggle() {
        visible = !visible;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui == null) return;
        int count = Points.size();
        String msg = visible
                ? "ReGraded markers on (" + count + " points)"
                : "ReGraded markers off";
        client.gui.setOverlayMessage(Component.literal(msg), false);
    }

    public static void tick(Minecraft client) {
        if (!visible || client.level == null || client.player == null) return;
        Vec3 eye = client.player.getEyePosition();
        for (SurveyPoint p : Points.list().points()) {
            double x = p.easting();
            double y = p.elevation();
            double z = -p.northing();
            if (eye.distanceToSqr(x, y, z) > 256 * 256) continue;
            for (int i = 0; i < 5; i++) {
                client.level.addParticle(
                        new DustParticleOptions(0xFFFFFFFF, 1.2f),
                        true, true,
                        x, y + 0.1 * i, z, 0.0, 0.0, 0.0);
            }
        }
    }
}
