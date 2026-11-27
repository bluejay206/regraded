package me.bluejay.regraded.bits;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Soft-dependency applicator for Chisels & Bits using the researched mutator API.
 * Falls back to vanilla block placement if C&B is not loaded.
 * <p>
 * API path (26.1+):
 * IChiselsAndBitsAPI.getInstance().getMutatorFactory().in(world, pos)
 *   .overrideInAreaTarget(new BlockInformation(state, Optional.empty()), localVec3)
 * where localVec3 is in [0,1) within the block.
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
                in = factoryClass.getMethod("in", LevelAccessor.class, BlockPos.class);

                Class<?> mutatorClass = Class.forName("mod.chiselsandbits.api.multistate.mutator.world.IWorldAreaMutator");
                // overrideInAreaTarget(BlockInformation, Vec3)
                blockInformationClass = Class.forName("mod.chiselsandbits.api.blockinformation.BlockInformation");
                overrideInAreaTarget = mutatorClass.getMethod("overrideInAreaTarget", blockInformationClass, Vec3.class);

                // AIR constant for clear
                airInfo = blockInformationClass.getField("AIR").get(null);
            } catch (Exception e) {
                // Reflection failed — treat as not available
                System.err.println("[ReGraded] C&B API reflection failed: " + e.getMessage());
            }
        }
    }

    public static boolean isAvailable() {
        return LOADED && overrideInAreaTarget != null;
    }

    /**
     * Writes a single bit at the local position inside the given block.
     * local is expected in [0,1)^3 relative to the block origin.
     */
    public static void writeBit(LevelAccessor world, BlockPos pos, Vec3 local, BlockState state) {
        if (!isAvailable()) {
            // Fallback: set the whole block (coarse)
            world.setBlock(pos, state, 3);
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
            // Silent fallback
            world.setBlock(pos, state, 3);
        }
    }

    public static void clearBit(LevelAccessor world, BlockPos pos, Vec3 local) {
        if (!isAvailable() || airInfo == null) return;
        try {
            Object api = getInstance.invoke(null);
            Object factory = getMutatorFactory.invoke(api);
            Object mutator = in.invoke(factory, world, pos);
            overrideInAreaTarget.invoke(mutator, airInfo, local);
        } catch (Exception ignored) {}
    }
}
