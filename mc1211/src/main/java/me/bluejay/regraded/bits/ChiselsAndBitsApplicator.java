package me.bluejay.regraded.bits;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
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
 * Reflection is resolved lazily on first use, and the API class is loaded from C&B's own
 * classloader. A plain Class.forName from ReGraded's classloader sees a different Class
 * object whose Holder is still null, even after C&B has installed the real API.
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

    private static ClassLoader cbClassLoader() {
        for (EntrypointContainer<Object> ep : FabricLoader.getInstance().getEntrypointContainers("main", Object.class)) {
            if (MOD_ID.equals(ep.getProvider().getMetadata().getId())) {
                Object entry = ep.getEntrypoint();
                if (entry != null) return entry.getClass().getClassLoader();
            }
        }
        for (EntrypointContainer<Object> ep : FabricLoader.getInstance().getEntrypointContainers("client", Object.class)) {
            if (MOD_ID.equals(ep.getProvider().getMetadata().getId())) {
                Object entry = ep.getEntrypoint();
                if (entry != null) return entry.getClass().getClassLoader();
            }
        }
        return null;
    }

    private static synchronized boolean ensureResolved() {
        if (resolved && available) return true;
        if (!LOADED) {
            resolved = true;
            available = false;
            lastFailure = "C&B mod not loaded";
            return false;
        }
        try {
            ClassLoader cl = cbClassLoader();
            if (cl == null) {
                lastFailure = "Could not find C&B classloader via entrypoints";
                return false;
            }
            Class<?> apiClass = Class.forName("mod.chiselsandbits.api.IChiselsAndBitsAPI", true, cl);
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

            Class<?> mutatorClass = Class.forName("mod.chiselsandbits.api.multistate.mutator.world.IWorldAreaMutator", true, cl);
            blockInformationClass = Class.forName("mod.chiselsandbits.api.blockinformation.BlockInformation", true, cl);
            overrideInAreaTarget = mutatorClass.getMethod("overrideInAreaTarget", blockInformationClass, Vec3d.class);
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
