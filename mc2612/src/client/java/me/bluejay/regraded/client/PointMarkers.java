package me.bluejay.regraded.client;

import me.bluejay.regraded.core.Points;
import me.bluejay.regraded.core.SurveyPoint;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * White 0.1 x 0.5 outline boxes at imported survey points, plus white lines
 * between nearest-neighbor points to show the TIN edges.
 * Coordinates: x = easting, y = elevation, z = -northing (same as LevelHeaded).
 * Same outline style as LevelHeaded mc1211 WorldRenderEvents boxes.
 */
public final class PointMarkers {
    private static final double HALF = 0.05;
    private static final double HEIGHT = 0.5;
    private static final int NEAREST = 3;
    private static boolean visible;
    private static boolean renderRegistered;

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

    public static void registerRender() {
        if (renderRegistered) return;
        renderRegistered = true;
        LevelRenderEvents.BEFORE_GIZMOS.register(PointMarkers::render);
    }

    private static void render(LevelRenderContext context) {
        if (!visible) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameRenderer == null) return;
        List<SurveyPoint> points = Points.list().points();
        if (points.isEmpty()) return;

        Vec3 cam = client.gameRenderer.getMainCamera().getPosition();
        PoseStack pose = context.poseStack();
        MultiBufferSource.BufferSource buffers = context.bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        for (SurveyPoint p : points) {
            double x = p.easting();
            double y = p.elevation();
            double z = -p.northing();
            if (cam.distanceToSqr(x, y, z) > 256 * 256) continue;
            LevelRenderer.renderLineBox(
                    pose, lines,
                    x - cam.x - HALF, y - cam.y, z - cam.z - HALF,
                    x - cam.x + HALF, y - cam.y + HEIGHT, z - cam.z + HALF,
                    1.0f, 1.0f, 1.0f, 1.0f);
        }

        drawTin(points, cam, pose, lines);
        buffers.endBatch(RenderType.lines());
    }

    /** White lines from each point to its nearest neighbors. */
    private static void drawTin(List<SurveyPoint> points, Vec3 cam, PoseStack pose, VertexConsumer lines) {
        Set<Long> drawn = new HashSet<>();
        int n = points.size();
        for (int i = 0; i < n; i++) {
            SurveyPoint a = points.get(i);
            double ax = a.easting(), ay = a.elevation(), az = -a.northing();
            if (cam.distanceToSqr(ax, ay, az) > 256 * 256) continue;
            int[] nearest = new int[NEAREST];
            double[] dist = new double[NEAREST];
            for (int k = 0; k < NEAREST; k++) { nearest[k] = -1; dist[k] = Double.MAX_VALUE; }
            for (int j = 0; j < n; j++) {
                if (j == i) continue;
                SurveyPoint b = points.get(j);
                double d = distSq(a, b);
                for (int k = 0; k < NEAREST; k++) {
                    if (d < dist[k]) {
                        for (int m = NEAREST - 1; m > k; m--) { dist[m] = dist[m - 1]; nearest[m] = nearest[m - 1]; }
                        dist[k] = d;
                        nearest[k] = j;
                        break;
                    }
                }
            }
            for (int k = 0; k < NEAREST; k++) {
                int j = nearest[k];
                if (j < 0) continue;
                long key = edgeKey(i, j);
                if (!drawn.add(key)) continue;
                SurveyPoint b = points.get(j);
                line(pose, lines, cam, ax, ay, az, b.easting(), b.elevation(), -b.northing());
            }
        }
    }

    private static void line(PoseStack pose, VertexConsumer lines, Vec3 cam,
                             double x1, double y1, double z1, double x2, double y2, double z2) {
        float dx = (float) (x2 - x1);
        float dy = (float) (y2 - y1);
        float dz = (float) (z2 - z1);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-4f) return;
        float nx = dx / len, ny = dy / len, nz = dz / len;
        var poseEntry = pose.last();
        lines.addVertex(poseEntry, (float) (x1 - cam.x), (float) (y1 - cam.y), (float) (z1 - cam.z))
                .setColor(1.0f, 1.0f, 1.0f, 1.0f)
                .setNormal(poseEntry, nx, ny, nz);
        lines.addVertex(poseEntry, (float) (x2 - cam.x), (float) (y2 - cam.y), (float) (z2 - cam.z))
                .setColor(1.0f, 1.0f, 1.0f, 1.0f)
                .setNormal(poseEntry, nx, ny, nz);
    }

    private static double distSq(SurveyPoint a, SurveyPoint b) {
        double dx = a.easting() - b.easting();
        double dz = a.northing() - b.northing();
        return dx * dx + dz * dz;
    }

    private static long edgeKey(int i, int j) {
        int lo = Math.min(i, j), hi = Math.max(i, j);
        return ((long) lo << 32) | (hi & 0xFFFFFFFFL);
    }

    public static void tick(Minecraft client) {
        // Overlay only. Boxes and TIN lines are drawn in the render event.
    }
}
