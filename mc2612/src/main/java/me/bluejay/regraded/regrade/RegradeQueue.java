package me.bluejay.regraded.regrade;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Tick-driven column queue. Regrade work is enqueued and drained a few
 * columns per server tick so C&B's mutator buffer stays empty between
 * targets. Each column opens one batch, writes its bits, and closes it
 * before the next column starts.
 */
public final class RegradeQueue {
    private static final int COLUMNS_PER_TICK = 4;
    private static final Queue<Job> QUEUE = new ArrayDeque<>();
    private static boolean registered;
    private static int written;
    private static ServerPlayer reporter;

    private RegradeQueue() {}

    public static int size() { return QUEUE.size(); }
    public static boolean isEmpty() { return QUEUE.isEmpty(); }

    public static void enqueueColumn(ServerLevel world, int x, int z, ServerPlayer reporter) {
        QUEUE.add(new Job(world, x, z));
        RegradeQueue.reporter = reporter;
        ensureRegistered();
    }

    public static void enqueueChunk(ServerLevel world, ChunkPos chunk, ServerPlayer reporter) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                QUEUE.add(new Job(world, (chunk.x() << 4) + x, (chunk.z() << 4) + z));
            }
        }
        RegradeQueue.reporter = reporter;
        ensureRegistered();
    }

    public static void clear() {
        QUEUE.clear();
        written = 0;
    }

    private static void ensureRegistered() {
        if (registered) return;
        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(RegradeQueue::tick);
    }

    private static void tick(MinecraftServer server) {
        if (QUEUE.isEmpty()) return;
        if (written == 0) Regrade.resetStats();
        int done = 0;
        while (!QUEUE.isEmpty() && done < COLUMNS_PER_TICK) {
            Job job = QUEUE.poll();
            try {
                written += Regrade.column(job.world, job.x, job.z);
            } catch (Exception e) {
                if (reporter != null) {
                    reporter.sendSystemMessage(Component.literal(
                            "Regrade error at " + job.x + "," + job.z + ": " + e.getMessage()));
                }
            }
            done++;
        }
        if (QUEUE.isEmpty() && reporter != null) {
            String range = Regrade.elevationRange();
            String fail = Regrade.failure();
            String msg = "Regrade complete (" + written + " bits)" + range
                    + (fail.isEmpty() ? "" : " [" + fail + "]") + ".";
            reporter.sendSystemMessage(Component.literal(msg));
            written = 0;
            reporter = null;
        }
    }

    private record Job(ServerLevel world, int x, int z) {}
}
