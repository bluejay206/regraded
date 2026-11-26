package me.bluejay.regraded;

import me.bluejay.regraded.core.PointList;
import me.bluejay.regraded.core.SurveyPoint;

/**
 * Pure surface model built from an independent PointList.
 * Current stub: nearest-neighbor height. Future: TIN or averaged grid.
 */
public final class SurfaceModel {
    private final PointList points;

    public SurfaceModel(PointList points) {
        this.points = points == null ? new PointList() : points;
    }

    /** Height at (easting, northing) in survey space. */
    public double heightAt(double easting, double northing) {
        SurveyPoint best = null;
        double bestDist = Double.MAX_VALUE;
        for (SurveyPoint p : points.points()) {
            double de = p.easting() - easting;
            double dn = p.northing() - northing;
            double d = de * de + dn * dn;
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best == null ? 64.0 : best.elevation();
    }

    public PointList points() {
        return points;
    }
}
