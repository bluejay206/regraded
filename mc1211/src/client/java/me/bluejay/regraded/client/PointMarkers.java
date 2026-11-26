package me.bluejay.regraded.client;

import me.bluejay.regraded.core.Points;
import me.bluejay.regraded.core.SurveyPoint;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ctrl-Z overlay: white particle column at each point, plus TIN edge lines
 * drawn at the surface elevations of the endpoints.
 * Coordinate convention: easting = X, northing = -Z.
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
        WorldRenderEvents.AFTER_ENTITIES.register(PointMarkers::renderTin);
    }

    private static void renderTin(WorldRenderContext context) {
        if (!visible) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        if (context.matrixStack() == null || context.consumers() == null) return;
        List<SurveyPoint> pts = Points.list().points();
        if (pts.size() < 2) return;

        Vec3d cam = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer lines = context.consumers().getBuffer(RenderLayer.getLines());

        List<int[]> edges = tinEdges(pts);
        for (int[] e : edges) {
            SurveyPoint a = pts.get(e[0]);
            SurveyPoint b = pts.get(e[1]);
            float ax = (float) (a.easting() - cam.x);
            float ay = (float) (a.elevation() - cam.y);
            float az = (float) (-a.northing() - cam.z);
            float bx = (float) (b.easting() - cam.x);
            float by = (float) (b.elevation() - cam.y);
            float bz = (float) (-b.northing() - cam.z);
            float dx = bx - ax, dy = by - ay, dz = bz - az;
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1e-4f) continue;
            float nx = dx / len, ny = dy / len, nz = dz / len;
            lines.vertex(matrix, ax, ay, az).color(1f, 1f, 1f, 0.9f).normal(nx, ny, nz);
            lines.vertex(matrix, bx, by, bz).color(1f, 1f, 1f, 0.9f).normal(nx, ny, nz);
        }
    }

    /** Nearest-neighbor edges: each point connects to its two closest neighbors. */
    private static List<int[]> tinEdges(List<SurveyPoint> pts) {
        Set<String> seen = new HashSet<>();
        List<int[]> edges = new ArrayList<>();
        int n = pts.size();
        for (int i = 0; i < n; i++) {
            SurveyPoint p = pts.get(i);
            int n1 = -1, n2 = -1;
            double d1 = Double.MAX_VALUE, d2 = Double.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                SurveyPoint q = pts.get(j);
                double dx = p.easting() - q.easting();
                double dz = p.northing() - q.northing();
                double d = dx * dx + dz * dz;
                if (d < d1) { d2 = d1; n2 = n1; d1 = d; n1 = j; }
                else if (d < d2) { d2 = d; n2 = j; }
            }
            if (n1 >= 0) addEdge(seen, edges, i, n1);
            if (n2 >= 0) addEdge(seen, edges, i, n2);
        }
        return edges;
    }

    private static void addEdge(Set<String> seen, List<int[]> edges, int a, int b) {
        int lo = Math.min(a, b), hi = Math.max(a, b);
        String key = lo + ":" + hi;
        if (seen.add(key)) edges.add(new int[]{lo, hi});
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
