package me.bluejay.regraded.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Independent in-memory point list for ReGraded.
 * Supports basic PNEZD CSV import (compatible with LevelHeaded exports).
 */
public final class PointList {
    private final List<SurveyPoint> points = new ArrayList<>();
    private static final int MAX_ROWS = 50_000;

    public void add(SurveyPoint p) {
        if (p != null) points.add(p);
    }

    public List<SurveyPoint> points() {
        return Collections.unmodifiableList(points);
    }

    public int size() {
        return points.size();
    }

    public void clear() {
        points.clear();
    }

    /**
     * Import PNEZD-style CSV.
     * Expected columns: Point, Northing, Easting, Elevation, Description (optional).
     * Header row is skipped. Returns number of points added.
     */
    public int importCsv(String csv) {
        if (csv == null || csv.isBlank()) return 0;
        int added = 0;
        String[] lines = csv.split("\\R");
        for (String line : lines) {
            if (added >= MAX_ROWS) break;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.toLowerCase().startsWith("point")) continue;
            String[] c = trimmed.split(",", -1);
            if (c.length < 4) continue;
            try {
                String id = c[0].trim();
                if (id.isEmpty() || id.length() > 64) continue;
                double northing = Double.parseDouble(c[1].trim());
                double easting = Double.parseDouble(c[2].trim());
                double elevation = Double.parseDouble(c[3].trim());
                String desc = c.length > 4 ? c[4].trim() : "";
                if (desc.length() > 256) desc = desc.substring(0, 256);
                add(new SurveyPoint(id, easting, northing, elevation, desc));
                added++;
            } catch (NumberFormatException ignored) {
                // skip bad row
            }
        }
        return added;
    }
}
