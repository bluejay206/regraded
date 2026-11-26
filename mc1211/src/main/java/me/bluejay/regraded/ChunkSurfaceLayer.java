package me.bluejay.regraded;

import net.minecraft.util.math.ChunkPos;

/**
 * Per-chunk modified surface layer (Yarn).
 */
public final class ChunkSurfaceLayer {
    private final ChunkPos pos;
    private SurfaceModel model;
    private boolean modified;

    public ChunkSurfaceLayer(ChunkPos pos) {
        this.pos = pos;
    }

    public void apply(SurfaceModel model) {
        this.model = model;
        this.modified = true;
    }

    public boolean isModified() {
        return modified;
    }

    public void clear() {
        this.modified = false;
        this.model = null;
    }

    public ChunkPos pos() {
        return pos;
    }

    public SurfaceModel model() {
        return model;
    }
}
