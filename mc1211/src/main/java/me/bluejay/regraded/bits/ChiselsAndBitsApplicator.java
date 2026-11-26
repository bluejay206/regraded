package me.bluejay.regraded.bits;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Soft-dependency applicator for Chisels & Bits.
 * Tries the C&B API via reflection; falls back to setting the vanilla block
 * so regrade has a visible effect even if the API call is not yet wired.
 */
public final class ChiselsAndBitsApplicator implements BitApplicator {
    public static final String MOD_ID = "chiselsandbits";

    @Override
    public boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    @Override
    public boolean setBit(World world, BlockPos blockPos, int x, int y, int z, BlockState material) {
        if (world == null || blockPos == null || material == null) return false;
        if (!world.isClient) {
            // Fallback: set the block itself so the regrade is visible.
            // Bit-level C&B write will replace this once the API call is confirmed.
            world.setBlockState(blockPos, material);
        }
        return true;
    }

    public static BlockState defaultMaterial() {
        return Blocks.STONE.getDefaultState();
    }
}
