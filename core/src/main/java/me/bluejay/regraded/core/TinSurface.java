package me.bluejay.regraded.core;

import java.util.List;

/**
 * Inverse-distance surface over the nearest survey points.
 * Always varies when the points have different elevations; no triangle
 * rejection that collapses to a constant fallback.
 */
public final class TinSurface {
    private static final int NEAREST = 8;

    private TinSurface() {}

    public static double elevationAt(List<SurveyPoint> points, double easting, double northing) {
        if (points == null || points.isEmpty()) return Double.NaN;
        if (points.size() == 1) return points.get(0).elevation();

        SurveyPoint[] nearest = new SurveyPoint[NEAREST];
        double[] dist = new double[NEAREST];
        for (int i = 0; i < NEAREST; i++) dist[i] = Double.MAX_VALUE;
        int found = 0;

        for (SurveyPoint p : points) {
            double d = dist2(p.easting(), p.northing(), easting, northing);
            if (d < 1e-8) return p.elevation();
            if (found < NEAREST) {
                nearest[found] = p;
                dist[found] = d;
                found++;
                for (int i = found - 1; i > 0 && dist[i] < dist[i - 1]; i--) {
                    swap(nearest, dist, i, i - 1);
                }
            } else if (d < dist[found - 1]) {
                nearest[found - 1] = p;
                dist[found - 1] = d;
                for (int i = found - 1; i > 0 && dist[i] < dist[i - 1]; i--) {
                    swap(nearest, dist, i, i - 1);
                }
            }
        }

        double wSum = 0.0;
        double eSum = 0.0;
        for (int i = 0; i < found; i++) {
            double w = 1.0 / Math.max(dist[i], 1e-6);
            wSum += w;
            eSum += w * nearest[i].elevation();
        }
        return wSum == 0.0 ? Double.NaN : eSum / wSum;
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

    private static void swap(SurveyPoint[] pts, double[] dist, int i, int j) {
        SurveyPoint tp = pts[i]; pts[i] = pts[j]; pts[j] = tp;
        double td = dist[i]; dist[i] = dist[j]; dist[j] = td;
    }

    private static double dist2(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2, dy = y1 - y2;
        return dx * dx + dy * dy;
    }
}
