package me.bluejay.regraded.core;

/**
 * Shared in-memory point list for the current session.
 */
public final class Points {
    private static final PointList LIST = new PointList();

    private Points() {}

    public static PointList list() {
        return LIST;
    }

    public static int size() {
        return LIST.size();
    }

    public static void clear() {
        LIST.clear();
    }
}
