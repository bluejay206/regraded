package me.bluejay.regraded.core;

/**
 * Independent survey point for ReGraded.
 * Coordinates are easting / northing / elevation (survey space).
 */
public record SurveyPoint(
        String id,
        double easting,
        double northing,
        double elevation,
        String description
) {
    public SurveyPoint {
        description = description == null ? "" : description;
    }
}
