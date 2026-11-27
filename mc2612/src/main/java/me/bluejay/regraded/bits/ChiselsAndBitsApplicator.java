package me.bluejay.regraded.bits;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Soft-dependency applicator for Chisels & Bits (Mojang mappings / 26.1.2).
 * Implements BitApplicator so it can be installed at launch.
 * Uses reflection against the mutator API so the project builds without a hard C&B dependency.
 */
public final class ChiselsAndBitsApplicator implements BitApplicator {
    private static final String MOD_ID = "chiselsandbits";
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded(MOD_ID);
    private static final int BITS = 16;

    private static Method getInstance;
    private static Method getMutatorFactory;
    private static Method in;
    private static Method overrideInAreaTarget;
    private static Class<?> blockInformationClass;
    private static Object airInfo;
    private static String lastFailure = null;

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
                blockInformationClass = Class.forName("mod.chiselsandbits.api.blockinformation.BlockInformation");
                overrideInAreaTarget = mutatorClass.getMethod("overrideInAreaTarget", blockInformationClass, Vec3.class);
                airInfo = blockInformationClass.getField("AIR").get(null);
            } catch (Exception e) {
                lastFailure = "C&B API reflection failed: " + e.getMessage();
                System.err.println("[ReGraded] " + lastFailure);
            }
        }
    }

    public static boolean isAvailableStatic() {
        return LOADED && overrideInAreaTarget != null;
    }

    @Override
    public boolean isAvailable() {
        return isAvailableStatic();
    }

    public static BlockState defaultMaterial() {
        return Blocks.DIRT.defaultBlockState();
    }

    public static String lastFailure() {
        return lastFailure == null ? "" : lastFailure;
    }

    @Override
    public boolean setBit(Level world, BlockPos blockPos, int x, int y, int z, BlockState material) {
        if (!isAvailableStatic()) {
            lastFailure = "C&B not available";
            return false;
        }
        double lx = (x + 0.5) / BITS;
        double ly = (y + 0.5) / BITS;
        double lz = (z + 0.5) / BITS;
        Vec3 local = new Vec3(lx, ly, lz);
        try {
            Object api = getInstance.invoke(null);
            Object factory = getMutatorFactory.invoke(api);
            Object mutator = in.invoke(factory, world, blockPos);
            Object info = blockInformationClass.getConstructor(BlockState.class, Optional.class)
                    .newInstance(material, Optional.empty());
            overrideInAreaTarget.invoke(mutator, info, local);
            lastFailure = null;
            return true;
        } catch (Exception e) {
            lastFailure = e.getClass().getSimpleName() + ": " + e.getMessage();
            return false;
        }
    }
}
