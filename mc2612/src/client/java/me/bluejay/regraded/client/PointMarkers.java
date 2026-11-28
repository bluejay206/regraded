package me.bluejay.regraded.client;

import me.bluejay.regraded.core.Points;
import me.bluejay.regraded.core.SurveyPoint;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * White 0.1 x 0.5 outline boxes at imported survey points, plus white lines
 * for the edges of a 2D Delaunay triangulation (easting/northing).
 * Coordinates: x = easting, y = elevation, z = -northing (same as LevelHeaded).
 */
public final class PointMarkers {
    private static final double HALF = 0.05;
    private static final double HEIGHT = 0.5;
    private static boolean visible;
    private static boolean renderRegistered;

    // Cached Delaunay edges; rebuilt when the point list changes.
    private static List<int[]> cachedEdges = List.of();
    private static int cachedSize = -1;
    private static Object cachedList = null;

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

    /** White lines for the edges of a Delaunay triangulation of the points. */
    private static void drawTin(List<SurveyPoint> points, Vec3 cam, PoseStack pose, VertexConsumer lines) {
        List<int[]> edges = edgesFor(points);
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

    private static List<int[]> edgesFor(List<SurveyPoint> points) {
        if (points == cachedList && points.size() == cachedSize) return cachedEdges;
        cachedList = points;
        cachedSize = points.size();
        cachedEdges = delaunayEdges(points);
        return cachedEdges;
    }

    /**
     * Bowyer-Watson Delaunay triangulation on (easting, northing).
     * Returns unique edges as [i, j] index pairs. Super-triangle vertices are
     * discarded so the result covers the convex hull of the input points.
     */
    private static List<int[]> delaunayEdges(List<SurveyPoint> points) {
        int n = points.size();
        if (n < 2) return List.of();
        if (n == 2) return List.of(new int[]{0, 1});

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (SurveyPoint p : points) {
            double x = p.easting(), z = p.northing();
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (z < minZ) minZ = z;
            if (z > maxZ) maxZ = z;
        }
        double dx = Math.max(maxX - minX, 1.0);
        double dz = Math.max(maxZ - minZ, 1.0);
        double dmax = Math.max(dx, dz);
        double midX = (minX + maxX) * 0.5;
        double midZ = (minZ + maxZ) * 0.5;

        // Super-triangle large enough to contain all points.
        int sa = n, sb = n + 1, sc = n + 2;
        double[] sx = new double[n + 3];
        double[] sz = new double[n + 3];
        for (int i = 0; i < n; i++) {
            sx[i] = points.get(i).easting();
            sz[i] = points.get(i).northing();
        }
        sx[sa] = midX - 20 * dmax;
        sz[sa] = midZ - dmax;
        sx[sb] = midX;
        sz[sb] = midZ + 20 * dmax;
        sx[sc] = midX + 20 * dmax;
        sz[sc] = midZ - dmax;

        List<Tri> tris = new ArrayList<>();
        tris.add(new Tri(sa, sb, sc, sx, sz));

        for (int i = 0; i < n; i++) {
            double px = sx[i], pz = sz[i];
            List<Tri> bad = new ArrayList<>();
            for (Tri t : tris) {
                if (t.contains(px, pz)) bad.add(t);
            }
            Set<Long> boundary = new HashSet<>();
            for (Tri t : bad) {
                addOrRemove(boundary, t.a, t.b);
                addOrRemove(boundary, t.b, t.c);
                addOrRemove(boundary, t.c, t.a);
            }
            tris.removeAll(bad);
            for (long edge : boundary) {
                int u = (int) (edge >>> 32);
                int v = (int) edge;
                tris.add(new Tri(u, v, i, sx, sz));
            }
        }

        // Collect unique edges that do not touch the super-triangle.
        Set<Long> edges = new HashSet<>();
        List<int[]> result = new ArrayList<>();
        for (Tri t : tris) {
            if (t.a >= n || t.b >= n || t.c >= n) continue;
            addEdge(edges, result, t.a, t.b);
            addEdge(edges, result, t.b, t.c);
            addEdge(edges, result, t.c, t.a);
        }
        return result;
    }

    private static void addOrRemove(Set<Long> set, int i, int j) {
        long k = edgeKey(i, j);
        if (!set.add(k)) set.remove(k);
    }

    private static void addEdge(Set<Long> seen, List<int[]> out, int i, int j) {
        long k = edgeKey(i, j);
        if (seen.add(k)) out.add(new int[]{Math.min(i, j), Math.max(i, j)});
    }

    private static long edgeKey(int i, int j) {
        int lo = Math.min(i, j), hi = Math.max(i, j);
        return ((long) lo << 32) | (hi & 0xFFFFFFFFL);
    }

    /** Triangle with precomputed circumcircle. Degenerate circles have r2 = -1. */
    private static final class Tri {
        final int a, b, c;
        final double cx, cy, r2;

        Tri(int a, int b, int c, double[] x, double[] z) {
            this.a = a;
            this.b = b;
            this.c = c;
            double ax = x[a], az = z[a];
            double bx = x[b], bz = z[b];
            double cx0 = x[c], cz0 = z[c];
            double d = 2.0 * (ax * (bz - cz0) + bx * (cz0 - az) + cx0 * (az - bz));
            if (Math.abs(d) < 1e-12) {
                this.cx = 0;
                this.cy = 0;
                this.r2 = -1;
                return;
            }
            double a2 = ax * ax + az * az;
            double b2 = bx * bx + bz * bz;
            double c2 = cx0 * cx0 + cz0 * cz0;
            this.cx = (a2 * (bz - cz0) + b2 * (cz0 - az) + c2 * (az - bz)) / d;
            this.cy = (a2 * (cx0 - bx) + b2 * (ax - cx0) + c2 * (bx - ax)) / d;
            double dx = ax - this.cx, dz = az - this.cy;
            this.r2 = dx * dx + dz * dz;
        }

        boolean contains(double x, double z) {
            if (r2 < 0) return false;
            double dx = x - cx, dz = z - cy;
            return dx * dx + dz * dz <= r2 + 1e-6;
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
