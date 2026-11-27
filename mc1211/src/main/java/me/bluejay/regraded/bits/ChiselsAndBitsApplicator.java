package me.bluejay.regraded.bits;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Soft-dependency applicator for Chisels & Bits (Yarn mappings for 1.21.1).
 * Implements BitApplicator so it can be installed at launch.
 * Uses reflection against the mutator API so the project builds without a hard C&B dependency.
 *
 * Reflection is resolved lazily on first use so it does not run before C&B finishes
 * installing its API (the previous static initializer could see a null factory).
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
        if (resolved) return available;
        resolved = true;
        if (!LOADED) {
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
            in = factoryClass.getMethod("in", WorldAccess.class, BlockPos.class);

            Class<?> mutatorClass = Class.forName("mod.chiselsandbits.api.multistate.mutator.world.IWorldAreaMutator");
            blockInformationClass = Class.forName("mod.chiselsandbits.api.blockinformation.BlockInformation");
            overrideInAreaTarget = mutatorClass.getMethod("overrideInAreaTarget", blockInformationClass, Vec3d.class);
            available = true;
            lastFailure = null;
            return true;
        } catch (Exception e) {
            lastFailure = "C&B API reflection failed: " + e.getClass().getSimpleName() + ": " + e.getMessage();
            System.err.println("[ReGraded] " + lastFailure);
            available = false;
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
        return Blocks.DIRT.getDefaultState();
    }

    public static String lastFailure() {
        return lastFailure == null ? "" : lastFailure;
    }

    @Override
    public boolean setBit(World world, BlockPos blockPos, int x, int y, int z, BlockState material) {
        if (!ensureResolved()) {
            return false;
        }
        double lx = (x + 0.5) / BITS;
        double ly = (y + 0.5) / BITS;
        double lz = (z + 0.5) / BITS;
        Vec3d local = new Vec3d(lx, ly, lz);
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
