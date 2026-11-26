package me.bluejay.regraded.bits;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Abstraction over BitsAndChisels / Chisels & Bits bit-setting APIs.
 * Allows ReGraded to regrade surfaces at 16³ resolution without hard-coding one mod.
 */
public interface BitApplicator {
    boolean isAvailable();

    /**
     * Set a single bit (0-15) inside the block at worldPos.
     * @return true if the bit was written
     */
    boolean setBit(World world, BlockPos worldPos, int x, int y, int z, BlockState material);

    /** Optional refresh after a batch of setBit calls. */
    default void update(World world, BlockPos pos) {}

    /** No-op implementation used when no bit mod is present. */
    BitApplicator NONE = new BitApplicator() {
        @Override public boolean isAvailable() { return false; }
        @Override public boolean setBit(World w, BlockPos p, int x, int y, int z, BlockState s) { return false; }
    };

    static BitApplicator get() { return Holder.INSTANCE; }

    final class Holder {
        static BitApplicator INSTANCE = NONE;
        private Holder() {}
    }
}
