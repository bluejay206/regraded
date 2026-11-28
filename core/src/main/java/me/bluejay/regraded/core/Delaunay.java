package me.bluejay.regraded.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 2D Delaunay triangulation (Bowyer-Watson) on (easting, northing).
 * Shared by TinSurface (barycentric elevation) and PointMarkers (edge lines).
 */
public final class Delaunay {
    public record Triangle(int a, int b, int c) {}

    private final List<SurveyPoint> points;
    private final List<Triangle> triangles;

    private Delaunay(List<SurveyPoint> points, List<Triangle> triangles) {
        this.points = points;
        this.triangles = triangles;
    }

    public List<SurveyPoint> points() { return points; }
    public List<Triangle> triangles() { return triangles; }

    /** Unique edges as [i, j] index pairs. */
    public List<int[]> edges() {
        Set<Long> seen = new HashSet<>();
        List<int[]> out = new ArrayList<>();
        for (Triangle t : triangles) {
            addEdge(seen, out, t.a, t.b);
            addEdge(seen, out, t.b, t.c);
            addEdge(seen, out, t.c, t.a);
        }
        return out;
    }

    /**
     * Barycentric elevation of (easting, northing).
     * Returns NaN if the query is outside every triangle (outside the convex hull)
     * or the point set is empty.
     */
    public double elevationAt(double easting, double northing) {
        if (points.isEmpty()) return Double.NaN;
        if (points.size() == 1) return points.get(0).elevation();
        for (Triangle t : triangles) {
            SurveyPoint pa = points.get(t.a);
            SurveyPoint pb = points.get(t.b);
            SurveyPoint pc = points.get(t.c);
            double[] bary = barycentric(easting, northing, pa, pb, pc);
            if (bary == null) continue;
            return bary[0] * pa.elevation() + bary[1] * pb.elevation() + bary[2] * pc.elevation();
        }
        return Double.NaN;
    }

    /** Build a Delaunay triangulation of the given points. */
    public static Delaunay build(List<SurveyPoint> points) {
        if (points == null || points.isEmpty()) return new Delaunay(List.of(), List.of());
        List<SurveyPoint> copy = List.copyOf(points);
        int n = copy.size();
        if (n < 3) return new Delaunay(copy, List.of());

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (SurveyPoint p : copy) {
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

        int sa = n, sb = n + 1, sc = n + 2;
        double[] sx = new double[n + 3];
        double[] sz = new double[n + 3];
        for (int i = 0; i < n; i++) {
            sx[i] = copy.get(i).easting();
            sz[i] = copy.get(i).northing();
        }
        sx[sa] = midX - 20 * dmax;
        sz[sa] = midZ - dmax;
        sx[sb] = midX;
        sz[sb] = midZ + 20 * dmax;
        sx[sc] = midX + 20 * dmax;
        sz[sc] = midZ - dmax;

        List<Circ> tris = new ArrayList<>();
        tris.add(new Circ(sa, sb, sc, sx, sz));

        for (int i = 0; i < n; i++) {
            double px = sx[i], pz = sz[i];
            List<Circ> bad = new ArrayList<>();
            for (Circ t : tris) {
                if (t.contains(px, pz)) bad.add(t);
            }
            Set<Long> boundary = new HashSet<>();
            for (Circ t : bad) {
                xor(boundary, t.a, t.b);
                xor(boundary, t.b, t.c);
                xor(boundary, t.c, t.a);
            }
            tris.removeAll(bad);
            for (long edge : boundary) {
                int u = (int) (edge >>> 32);
                int v = (int) edge;
                tris.add(new Circ(u, v, i, sx, sz));
            }
        }

        List<Triangle> result = new ArrayList<>();
        for (Circ t : tris) {
            if (t.a >= n || t.b >= n || t.c >= n) continue;
            result.add(new Triangle(t.a, t.b, t.c));
        }
        return new Delaunay(copy, result);
    }

    /** Barycentric coordinates if (x,z) is inside triangle abc, else null. */
    private static double[] barycentric(double x, double z, SurveyPoint a, SurveyPoint b, SurveyPoint c) {
        double ax = a.easting(), az = a.northing();
        double bx = b.easting(), bz = b.northing();
        double cx = c.easting(), cz = c.northing();
        double v0x = cx - ax, v0z = cz - az;
        double v1x = bx - ax, v1z = bz - az;
        double v2x = x - ax, v2z = z - az;
        double den = v0x * v1z - v1x * v0z;
        if (Math.abs(den) < 1e-12) return null;
        double w1 = (v2x * v1z - v1x * v2z) / den;
        double w2 = (v0x * v2z - v2x * v0z) / den;
        double w0 = 1.0 - w1 - w2;
        double eps = -1e-6;
        if (w0 < eps || w1 < eps || w2 < eps) return null;
        return new double[]{w0, w2, w1}; // order matches a, b, c
    }

    private static void addEdge(Set<Long> seen, List<int[]> out, int i, int j) {
        long k = key(i, j);
        if (seen.add(k)) out.add(new int[]{Math.min(i, j), Math.max(i, j)});
    }

    private static void xor(Set<Long> set, int i, int j) {
        long k = key(i, j);
        if (!set.add(k)) set.remove(k);
    }

    private static long key(int i, int j) {
        int lo = Math.min(i, j), hi = Math.max(i, j);
        return ((long) lo << 32) | (hi & 0xFFFFFFFFL);
    }

    /** Triangle with precomputed circumcircle for the insertion step. */
    private static final class Circ {
        final int a, b, c;
        final double cx, cy, r2;

        Circ(int a, int b, int c, double[] x, double[] z) {
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
}
