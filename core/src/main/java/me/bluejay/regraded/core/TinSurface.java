package me.bluejay.regraded.core;

import java.util.List;

/**
 * Surface elevation from a Delaunay triangulation of the survey points.
 * Queries use barycentric interpolation inside the triangle that contains
 * the sample; outside the convex hull the nearest-point elevation is used.
 */
public final class TinSurface {
    private static List<SurveyPoint> cachedPoints = List.of();
    private static Delaunay cached = null;

    private TinSurface() {}

    public static double elevationAt(List<SurveyPoint> points, double easting, double northing) {
        if (points == null || points.isEmpty()) return Double.NaN;
        if (points.size() == 1) return points.get(0).elevation();

        Delaunay d = triangulation(points);
        double e = d.elevationAt(easting, northing);
        if (!Double.isNaN(e)) return e;

        // Outside the convex hull: fall back to the nearest point.
        SurveyPoint nearest = null;
        double best = Double.MAX_VALUE;
        for (SurveyPoint p : points) {
            double dx = p.easting() - easting;
            double dz = p.northing() - northing;
            double dist = dx * dx + dz * dz;
            if (dist < best) {
                best = dist;
                nearest = p;
            }
        }
        return nearest == null ? Double.NaN : nearest.elevation();
    }

    /** Elevation range of the loaded points, or NaN if empty. */
    public static double[] range(List<SurveyPoint> points) {
        if (points == null || points.isEmpty()) return new double[]{Double.NaN, Double.NaN};
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (SurveyPoint p : points) {
            double e = p.elevation();
            if (e < min) min = e;
            if (e > max) max = e;
        }
        return new double[]{min, max};
    }

    /** Shared triangulation so markers and elevation queries stay consistent. */
    public static Delaunay triangulation(List<SurveyPoint> points) {
        if (points == cachedPoints && cached != null) return cached;
        cachedPoints = points;
        cached = Delaunay.build(points);
        return cached;
    }
}
