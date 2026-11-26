package me.bluejay.regraded.bits;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Abstraction over a bit-level terrain applicator.
 * Concrete implementation is provided at launch when Chisels & Bits is present.
 */
public interface BitApplicator {
    boolean isAvailable();

    /**
     * Set a single bit inside a block.
     * x/y/z are local bit coordinates (0..15 for 16³ resolution).
     */
    boolean setBit(World world, BlockPos blockPos, int x, int y, int z, BlockState material);

    static BitApplicator current() {
        return Holder.INSTANCE;
    }

    static void set(BitApplicator applicator) {
        Holder.INSTANCE = applicator == null ? NONE : applicator;
    }

    BitApplicator NONE = new BitApplicator() {
        @Override public boolean isAvailable() { return false; }
        @Override public boolean setBit(World world, BlockPos blockPos, int x, int y, int z, BlockState material) {
            return false;
        }
    };

    final class Holder {
        private static BitApplicator INSTANCE = NONE;
        private Holder() {}
    }
}
