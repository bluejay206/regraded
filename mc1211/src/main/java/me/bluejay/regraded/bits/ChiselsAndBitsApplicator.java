package me.bluejay.regraded.bits;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Method;

/**
 * Soft-dependency applicator for Chisels & Bits.
 * Tries the C&B API via reflection; falls back to setting the vanilla block
 * so regrade has a visible effect even if the API call is not yet confirmed.
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
        if (world.isClient) return false;

        if (tryChiselsAndBits(world, blockPos, x, y, z, material)) {
            return true;
        }

        // Fallback: set the block itself so the regrade is visible.
        world.setBlockState(blockPos, material);
        return true;
    }

    private boolean tryChiselsAndBits(World world, BlockPos pos, int x, int y, int z, BlockState material) {
        try {
            // IChiselsAndBitsAPI.getInstance().getBitAccess(world, pos)
            Class<?> apiClass = Class.forName("mod.chiselsandbits.api.IChiselsAndBitsAPI");
            Method getInstance = apiClass.getMethod("getInstance");
            Object api = getInstance.invoke(null);
            if (api == null) return false;

            Method getBitAccess = api.getClass().getMethod("getBitAccess", World.class, BlockPos.class);
            Object access = getBitAccess.invoke(api, world, pos);
            if (access == null) return false;

            // setBitAt(int x, int y, int z, BlockInformation)
            Class<?> infoClass = Class.forName("mod.chiselsandbits.api.blockinformation.BlockInformation");
            Method create = infoClass.getMethod("create", BlockState.class);
            Object info = create.invoke(null, material);

            Method setBitAt = access.getClass().getMethod("setBitAt", int.class, int.class, int.class, infoClass);
            setBitAt.invoke(access, x, y, z, info);

            Method commit = access.getClass().getMethod("commitChanges", boolean.class);
            commit.invoke(access, true);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static BlockState defaultMaterial() {
        return Blocks.STONE.getDefaultState();
    }
}
