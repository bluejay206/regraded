package me.bluejay.regraded.core;

import java.util.List;

/**
 * Minimal TIN interpolator. Uses the three nearest points and barycentric
 * elevation when they form a usable triangle; otherwise inverse-distance.
 */
public final class TinSurface {
    private TinSurface() {}

    public static double elevationAt(List<SurveyPoint> points, double easting, double northing) {
        if (points == null || points.isEmpty()) return Double.NaN;
        if (points.size() == 1) return points.get(0).elevation();

        SurveyPoint a = null, b = null, c = null;
        double da = Double.MAX_VALUE, db = Double.MAX_VALUE, dc = Double.MAX_VALUE;
        for (SurveyPoint p : points) {
            double d = dist2(p.easting(), p.northing(), easting, northing);
            if (d < da) { c = b; dc = db; b = a; db = da; a = p; da = d; }
            else if (d < db) { c = b; dc = db; b = p; db = d; }
            else if (d < dc) { c = p; dc = d; }
        }
        if (a == null) return Double.NaN;
        if (b == null) return a.elevation();
        if (c == null) return idw(a, b, easting, northing);

        double elev = barycentric(a, b, c, easting, northing);
        return Double.isNaN(elev) ? idw3(a, b, c, easting, northing) : elev;
    }

    private static double barycentric(SurveyPoint a, SurveyPoint b, SurveyPoint c, double x, double y) {
        double den = (b.northing() - c.northing()) * (a.easting() - c.easting())
                + (c.easting() - b.easting()) * (a.northing() - c.northing());
        if (Math.abs(den) < 1e-9) return Double.NaN;
        double w1 = ((b.northing() - c.northing()) * (x - c.easting())
                + (c.easting() - b.easting()) * (y - c.northing())) / den;
        double w2 = ((c.northing() - a.northing()) * (x - c.easting())
                + (a.easting() - c.easting()) * (y - c.northing())) / den;
        double w3 = 1.0 - w1 - w2;
        if (w1 < -0.01 || w2 < -0.01 || w3 < -0.01) return Double.NaN;
        return w1 * a.elevation() + w2 * b.elevation() + w3 * c.elevation();
    }

    private static double idw(SurveyPoint a, SurveyPoint b, double x, double y) {
        double da = Math.max(0.001, dist2(a.easting(), a.northing(), x, y));
        double db = Math.max(0.001, dist2(b.easting(), b.northing(), x, y));
        return (a.elevation() / da + b.elevation() / db) / (1.0 / da + 1.0 / db);
    }

    private static double idw3(SurveyPoint a, SurveyPoint b, SurveyPoint c, double x, double y) {
        double da = Math.max(0.001, dist2(a.easting(), a.northing(), x, y));
        double db = Math.max(0.001, dist2(b.easting(), b.northing(), x, y));
        double dc = Math.max(0.001, dist2(c.easting(), c.northing(), x, y));
        return (a.elevation() / da + b.elevation() / db + c.elevation() / dc)
                / (1.0 / da + 1.0 / db + 1.0 / dc);
    }

    private static double dist2(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2, dy = y1 - y2;
        return dx * dx + dy * dy;
    }
}
