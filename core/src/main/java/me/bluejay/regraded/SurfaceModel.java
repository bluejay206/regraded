package me.bluejay.regraded;

import me.bluejay.surveyio.PointStore;
import me.bluejay.surveyio.SurveyPoint;

/**
 * Builds a height surface from survey points.
 * Current stub: simple average or nearest-neighbor placeholder.
 * Future: Delaunay TIN + barycentric interpolation.
 */
public final class SurfaceModel {
    private final PointStore store;

    public SurfaceModel(PointStore store) {
        this.store = store;
    }

    /** Height at (easting, northing) in survey space. Stub returns nearest point elevation. */
    public double heightAt(double easting, double northing) {
        SurveyPoint best = null;
        double bestDist = Double.MAX_VALUE;
        for (SurveyPoint p : store.points()) {
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

    public PointStore store() {
        return store;
    }
}
