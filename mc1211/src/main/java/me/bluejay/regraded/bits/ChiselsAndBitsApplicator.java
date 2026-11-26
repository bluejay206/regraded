package me.bluejay.regraded.bits;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Soft-dependency applicator for Chisels & Bits (modid: chiselsandbits).
 *
 * The concrete bit-write call is intentionally stubbed here. Once the C&B API
 * jar is on the compile classpath, replace the body of setBit with the real
 * IBitAccess / ChiselAndBitsAPI call.
 */
public final class ChiselsAndBitsApplicator implements BitApplicator {
    public static final String MOD_ID = "chiselsandbits";

    @Override
    public boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    @Override
    public boolean setBit(World world, BlockPos blockPos, int x, int y, int z, BlockState material) {
        if (!isAvailable()) return false;
        // TODO: call C&B API (IBitAccess / ChiselAndBitsAPI) once the dependency jar is available.
        // For now we report availability but do not mutate the world.
        return false;
    }
}
