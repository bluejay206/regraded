package me.bluejay.regraded.bits;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Abstraction over a bit-level terrain applicator (Mojang mappings / 26.1.2).
 * Concrete implementation is provided at launch when Chisels & Bits is present.
 */
public interface BitApplicator {
    boolean isAvailable();

    /**
     * Set a single bit inside a block.
     * x/y/z are local bit coordinates (0..15 for 16³ resolution).
     */
    boolean setBit(Level world, BlockPos blockPos, int x, int y, int z, BlockState material);

    /**
     * Clear a single bit inside a block (cut).
     * x/y/z are local bit coordinates (0..15).
     */
    default boolean clearBit(Level world, BlockPos blockPos, int x, int y, int z) {
        return false;
    }

    /**
     * Open a batch for one block. Subsequent setBit/clearBit calls on the same
     * position reuse the same mutator until {@link #endBatch()} is called.
     * This keeps C&B's internal command buffer from filling and dumping onto
     * the last target.
     */
    default void beginBatch(Level world, BlockPos blockPos) {}

    /** Commit and close the current batch, if one is open. */
    default void endBatch() {}

    static BitApplicator current() {
        return Holder.INSTANCE;
    }

    static void set(BitApplicator applicator) {
        Holder.INSTANCE = applicator == null ? NONE : applicator;
    }

    BitApplicator NONE = new BitApplicator() {
        @Override public boolean isAvailable() { return false; }
        @Override public boolean setBit(Level world, BlockPos blockPos, int x, int y, int z, BlockState material) {
            return false;
        }
    };

    final class Holder {
        private static BitApplicator INSTANCE = NONE;
        private Holder() {}
    }
}
