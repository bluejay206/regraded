package me.bluejay.regraded.client;

import me.bluejay.regraded.core.Points;
import me.bluejay.regraded.core.SurveyPoint;
import me.bluejay.regraded.core.TinSurface;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.List;

/**
 * White 0.1 x 0.5 outline boxes at imported survey points, plus white lines
 * for the edges of the shared Delaunay triangulation used by TinSurface.
 * Coordinates: x = easting, y = elevation, z = -northing (same as LevelHeaded).
 */
public final class PointMarkers {
    private static final double HALF = 0.05;
    private static final double HEIGHT = 0.5;
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

        Vec3 cam = client.gameRenderer.getMainCamera().position();
        PoseStack pose = context.poseStack();
        MultiBufferSource.BufferSource buffers = context.bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderTypes.lines());

        for (SurveyPoint p : points) {
            double x = p.easting();
            double y = p.elevation();
            double z = -p.northing();
            if (cam.distanceToSqr(x, y, z) > 256 * 256) continue;
            box(pose, lines, cam, x, y, z);
        }

        drawTin(points, cam, pose, lines);
        buffers.endBatch(RenderTypes.lines());
    }

    /** White lines for the edges of the shared Delaunay triangulation. */
    private static void drawTin(List<SurveyPoint> points, Vec3 cam, PoseStack pose, VertexConsumer lines) {
        List<int[]> edges = TinSurface.triangulation(points).edges();
        int n = points.size();
        for (int[] e : edges) {
            if (e[0] < 0 || e[0] >= n || e[1] < 0 || e[1] >= n) continue;
            SurveyPoint a = points.get(e[0]);
            SurveyPoint b = points.get(e[1]);
            double ax = a.easting(), ay = a.elevation(), az = -a.northing();
            double bx = b.easting(), by = b.elevation(), bz = -b.northing();
            if (cam.distanceToSqr(ax, ay, az) > 256 * 256 && cam.distanceToSqr(bx, by, bz) > 256 * 256) continue;
            line(pose, lines, cam, ax, ay, az, bx, by, bz);
        }
    }

    /** White 0.1 x 0.5 outline box at a point. */
    private static void box(PoseStack pose, VertexConsumer lines, Vec3 cam, double x, double y, double z) {
        double x0 = x - HALF, x1 = x + HALF;
        double y0 = y, y1 = y + HEIGHT;
        double z0 = z - HALF, z1 = z + HALF;
        line(pose, lines, cam, x0, y0, z0, x1, y0, z0);
        line(pose, lines, cam, x1, y0, z0, x1, y0, z1);
        line(pose, lines, cam, x1, y0, z1, x0, y0, z1);
        line(pose, lines, cam, x0, y0, z1, x0, y0, z0);
        line(pose, lines, cam, x0, y1, z0, x1, y1, z0);
        line(pose, lines, cam, x1, y1, z0, x1, y1, z1);
        line(pose, lines, cam, x1, y1, z1, x0, y1, z1);
        line(pose, lines, cam, x0, y1, z1, x0, y1, z0);
        line(pose, lines, cam, x0, y0, z0, x0, y1, z0);
        line(pose, lines, cam, x1, y0, z0, x1, y1, z0);
        line(pose, lines, cam, x1, y0, z1, x1, y1, z1);
        line(pose, lines, cam, x0, y0, z1, x0, y1, z1);
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
                .setNormal(poseEntry, nx, ny, nz)
                .setLineWidth(1.0f);
        lines.addVertex(poseEntry, (float) (x2 - cam.x), (float) (y2 - cam.y), (float) (z2 - cam.z))
                .setColor(1.0f, 1.0f, 1.0f, 1.0f)
                .setNormal(poseEntry, nx, ny, nz)
                .setLineWidth(1.0f);
    }

    public static void tick(Minecraft client) {
        // Overlay only. Boxes and TIN lines are drawn in the render event.
    }
}
