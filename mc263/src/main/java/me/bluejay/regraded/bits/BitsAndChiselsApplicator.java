package me.bluejay.regraded.bits;

import io.github.coolmineman.bitsandchisels.api.BitUtils;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class BitsAndChiselsApplicator implements BitApplicator {
    @Override public boolean isAvailable() { return true; }

    @Override
    public boolean setBit(World world, BlockPos worldPos, int x, int y, int z, BlockState material) {
        if (world == null || worldPos == null || material == null) return false;
        if (x < 0 || x > 15 || y < 0 || y > 15 || z < 0 || z > 15) return false;
        return BitUtils.setBit(world, worldPos, x, y, z, material);
    }
}
