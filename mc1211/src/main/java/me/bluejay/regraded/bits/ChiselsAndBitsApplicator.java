package me.bluejay.regraded.bits;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Soft-dependency applicator for Chisels & Bits (Yarn mappings for 1.21.1).
 * Uses reflection against the C&B API so the project builds without a hard dependency.
 */
public final class ChiselsAndBitsApplicator {
    private static final String MOD_ID = "chiselsandbits";
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded(MOD_ID);

    private static Method getInstance;
    private static Method getMutatorFactory;
    private static Method in;
    private static Method overrideInAreaTarget;
    private static Class<?> blockInformationClass;
    private static Object airInfo;

    static {
        if (LOADED) {
            try {
                Class<?> apiClass = Class.forName("mod.chiselsandbits.api.IChiselsAndBitsAPI");
                getInstance = apiClass.getMethod("getInstance");
                Object api = getInstance.invoke(null);
                getMutatorFactory = apiClass.getMethod("getMutatorFactory");
                Object factory = getMutatorFactory.invoke(api);
                Class<?> factoryClass = factory.getClass();
                in = factoryClass.getMethod("in", WorldAccess.class, BlockPos.class);

                Class<?> mutatorClass = Class.forName("mod.chiselsandbits.api.multistate.mutator.world.IWorldAreaMutator");
                blockInformationClass = Class.forName("mod.chiselsandbits.api.blockinformation.BlockInformation");
                overrideInAreaTarget = mutatorClass.getMethod("overrideInAreaTarget", blockInformationClass, Vec3d.class);
                airInfo = blockInformationClass.getField("AIR").get(null);
            } catch (Exception e) {
                System.err.println("[ReGraded] C&B API reflection failed: " + e.getMessage());
            }
        }
    }

    public static boolean isAvailable() {
        return LOADED && overrideInAreaTarget != null;
    }

    public static void writeBit(WorldAccess world, BlockPos pos, Vec3d local, BlockState state) {
        if (!isAvailable()) {
            world.setBlockState(pos, state, 3);
            return;
        }
        try {
            Object api = getInstance.invoke(null);
            Object factory = getMutatorFactory.invoke(api);
            Object mutator = in.invoke(factory, world, pos);
            Object info = blockInformationClass.getConstructor(BlockState.class, Optional.class)
                    .newInstance(state, Optional.empty());
            overrideInAreaTarget.invoke(mutator, info, local);
        } catch (Exception e) {
            world.setBlockState(pos, state, 3);
        }
    }
}
