package me.bluejay.regraded.bits;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface BitApplicator {
    boolean isAvailable();
    boolean setBit(World world, BlockPos worldPos, int x, int y, int z, BlockState material);
    default void update(World world, BlockPos pos) {}

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
