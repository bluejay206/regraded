package me.bluejay.regraded.bits;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Method;

/**
 * Soft-dependency applicator for Chisels & Bits.
 * On failure the last reason is available via lastFailure().
 */
public final class ChiselsAndBitsApplicator implements BitApplicator {
    public static final String MOD_ID = "chiselsandbits";
    private static String lastFailure = "";
    private static boolean listed;

    @Override
    public boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    @Override
    public boolean setBit(World world, BlockPos blockPos, int x, int y, int z, BlockState material) {
        if (world == null || blockPos == null || material == null) return false;
        if (world.isClient) return false;
        return tryChiselsAndBits(world, blockPos, x, y, z, material);
    }

    public static String lastFailure() { return lastFailure; }

    private boolean tryChiselsAndBits(World world, BlockPos pos, int x, int y, int z, BlockState material) {
        try {
            Class<?> apiClass = Class.forName("mod.chiselsandbits.api.IChiselsAndBitsAPI");
            Method getInstance = apiClass.getMethod("getInstance");
            Object api = getInstance.invoke(null);
            if (api == null) {
                lastFailure = "API getInstance returned null";
                return false;
            }

            if (!listed) {
                listed = true;
                StringBuilder sb = new StringBuilder("API methods: ");
                for (Method m : api.getClass().getMethods()) {
                    if (m.getDeclaringClass() == Object.class) continue;
                    sb.append(m.getName()).append('(');
                    Class<?>[] params = m.getParameterTypes();
                    for (int i = 0; i < params.length; i++) {
                        if (i > 0) sb.append(',');
                        sb.append(params[i].getSimpleName());
                    }
                    sb.append(") ");
                }
                lastFailure = sb.toString();
                return false;
            }

            lastFailure = "methods listed; no write yet";
            return false;
        } catch (Throwable t) {
            lastFailure = t.getClass().getSimpleName() + ": " + t.getMessage();
            return false;
        }
    }

    public static BlockState defaultMaterial() {
        return Blocks.STONE.getDefaultState();
    }
}
