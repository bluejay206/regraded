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
 *
 * Reflection is resolved lazily on first use. Do not resolve during onInitialize, and do not
 * load the API through a different classloader: on 26.1.2 everything is on Knot, and a second
 * Class.forName(name, true, otherLoader) or getEntrypointContainers call disturbs C&B's static
 * Holder/config (seen as "Can not setup API twice!" and a null client configuration).
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
    private static volatile boolean resolved = false;
    private static volatile boolean available = false;
    private static String lastFailure = null;

    private static synchronized boolean ensureResolved() {
        if (resolved && available) return true;
        if (!LOADED) {
            resolved = true;
            available = false;
            lastFailure = "C&B mod not loaded";
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("mod.chiselsandbits.api.IChiselsAndBitsAPI");
            getInstance = apiClass.getMethod("getInstance");
            Object api = getInstance.invoke(null);
            if (api == null) {
                lastFailure = "IChiselsAndBitsAPI.getInstance() returned null (API not yet installed)";
                return false;
            }
            getMutatorFactory = apiClass.getMethod("getMutatorFactory");
            Object factory = getMutatorFactory.invoke(api);
            if (factory == null) {
                lastFailure = "getMutatorFactory() returned null";
                return false;
            }
            Class<?> factoryClass = factory.getClass();
            in = factoryClass.getMethod("in", LevelAccessor.class, BlockPos.class);

            Class<?> mutatorClass = Class.forName("mod.chiselsandbits.api.multistate.mutator.world.IWorldAreaMutator");
            blockInformationClass = Class.forName("mod.chiselsandbits.api.blockinformation.BlockInformation");
            overrideInAreaTarget = mutatorClass.getMethod("overrideInAreaTarget", blockInformationClass, Vec3.class);
            resolved = true;
            available = true;
            lastFailure = null;
            return true;
        } catch (Exception e) {
            lastFailure = "C&B API reflection failed: " + e.getClass().getSimpleName() + ": " + e.getMessage();
            System.err.println("[ReGraded] " + lastFailure);
            return false;
        }
    }

    public static boolean isAvailableStatic() {
        return ensureResolved();
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
        if (!ensureResolved()) {
            return false;
        }
        double lx = (x + 0.5) / BITS;
        double ly = (y + 0.5) / BITS;
        double lz = (z + 0.5) / BITS;
        Vec3 local = new Vec3(lx, ly, lz);
        try {
            Object api = getInstance.invoke(null);
            Object factory = getMutatorFactory.invoke(api);
            if (factory == null) {
                lastFailure = "getMutatorFactory() returned null at setBit time";
                return false;
            }
            Object mutator = in.invoke(factory, world, blockPos);
            if (mutator == null) {
                lastFailure = "IMutatorFactory.in() returned null";
                return false;
            }
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
