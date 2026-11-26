package me.bluejay.regraded;

import net.minecraft.util.math.ChunkPos;

/**
 * Per-chunk modified surface layer.
 * Stores the SurfaceModel application result so it can be applied or cleared reversibly.
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
        // TODO: call BitApplicator to write 16³ surface into the chunk's surface blocks
    }

    public boolean isModified() {
        return modified;
    }

    public void clear() {
        this.modified = false;
        this.model = null;
        // TODO: restore original blocks or clear bits
    }

    public ChunkPos pos() {
        return pos;
    }

    public SurfaceModel model() {
        return model;
    }
}
