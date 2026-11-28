package me.bluejay.regraded.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import me.bluejay.regraded.JobStore;
import me.bluejay.regraded.core.Points;
import me.bluejay.regraded.regrade.Regrade;
import me.bluejay.regraded.regrade.RegradeQueue;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * /rg command tree (Mojang mappings / 26.1.2).
 */
public final class RgCommands {
    private static final double RAY_RANGE = 100.0;

    private RgCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("rg")
                        .then(Commands.literal("import")
                                .then(Commands.argument("path", StringArgumentType.greedyString())
                                        .executes(ctx -> importCsv(ctx.getSource(), StringArgumentType.getString(ctx, "path")))))
                        .then(Commands.literal("regrade")
                                .then(Commands.literal("block")
                                        .executes(ctx -> regradeBlock(ctx.getSource()))
                                        .then(Commands.literal("raycast")
                                                .executes(ctx -> regradeBlockRaycast(ctx.getSource()))))
                                .then(Commands.literal("chunk")
                                        .executes(ctx -> regradeChunk(ctx.getSource()))
                                        .then(Commands.literal("raycast")
                                                .executes(ctx -> regradeChunkRaycast(ctx.getSource())))))
                        .then(Commands.literal("clear")
                                .executes(ctx -> {
                                    Points.clear();
                                    ensureWorld(ctx.getSource());
                                    JobStore.save();
                                    ctx.getSource().sendSuccess(() -> Component.literal("Point list cleared."), false);
                                    return 1;
                                }))
                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "Queue: " + RegradeQueue.size() + " columns"), false);
                                    return 1;
                                }))
                        .then(Commands.literal("cancel")
                                .executes(ctx -> {
                                    int n = RegradeQueue.size();
                                    RegradeQueue.clear();
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "Cancelled " + n + " queued columns."), false);
                                    return 1;
                                }))
                        .then(Commands.literal("count")
                                .executes(ctx -> {
                                    ctx.getSource().sendSuccess(() -> Component.literal("Points: " + Points.size()), false);
                                    return 1;
                                }))));
    }

    private static int importCsv(CommandSourceStack source, String pathStr) {
        Path path = Path.of(pathStr);
        if (!Files.isRegularFile(path)) {
            source.sendFailure(Component.literal("File not found: " + path));
            return 0;
        }
        try {
            String csv = Files.readString(path);
            int added = Points.list().importCsv(csv);
            ensureWorld(source);
            JobStore.save();
            source.sendSuccess(() -> Component.literal("Imported " + added + " points (total " + Points.size() + ") from " + path.getFileName()), false);
            return added;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Import failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int regradeBlock(CommandSourceStack source) {
        if (Points.size() == 0) {
            source.sendFailure(Component.literal("No points loaded. Use /rg import <csv> first."));
            return 0;
        }
        ServerLevel world = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        RegradeQueue.enqueueColumn(world, pos.getX(), pos.getZ(), source.getPlayer());
        source.sendSuccess(() -> Component.literal("Queued column at " + pos.getX() + "," + pos.getZ() + " (" + RegradeQueue.size() + " in queue)."), false);
        return 1;
    }

    private static int regradeBlockRaycast(CommandSourceStack source) {
        if (Points.size() == 0) {
            source.sendFailure(Component.literal("No points loaded. Use /rg import <csv> first."));
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Raycast requires a player."));
            return 0;
        }
        HitResult hit = player.pick(RAY_RANGE, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            source.sendFailure(Component.literal("No block in look direction."));
            return 0;
        }
        ServerLevel world = source.getLevel();
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        RegradeQueue.enqueueColumn(world, pos.getX(), pos.getZ(), player);
        source.sendSuccess(() -> Component.literal("Queued column under raycast at " + pos.getX() + "," + pos.getZ() + " (" + RegradeQueue.size() + " in queue)."), false);
        return 1;
    }

    private static int regradeChunk(CommandSourceStack source) {
        if (Points.size() == 0) {
            source.sendFailure(Component.literal("No points loaded. Use /rg import <csv> first."));
            return 0;
        }
        ServerLevel world = source.getLevel();
        BlockPos playerPos = BlockPos.containing(source.getPosition());
        ChunkPos chunk = new ChunkPos(playerPos.getX() >> 4, playerPos.getZ() >> 4);
        RegradeQueue.enqueueChunk(world, chunk, source.getPlayer());
        source.sendSuccess(() -> Component.literal("Queued chunk " + chunk.x() + "," + chunk.z() + " (" + RegradeQueue.size() + " columns)."), false);
        return 1;
    }

    private static int regradeChunkRaycast(CommandSourceStack source) {
        if (Points.size() == 0) {
            source.sendFailure(Component.literal("No points loaded. Use /rg import <csv> first."));
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Raycast requires a player."));
            return 0;
        }
        HitResult hit = player.pick(RAY_RANGE, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            source.sendFailure(Component.literal("No block in look direction."));
            return 0;
        }
        ServerLevel world = source.getLevel();
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        ChunkPos chunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        RegradeQueue.enqueueChunk(world, chunk, player);
        source.sendSuccess(() -> Component.literal("Queued chunk under raycast " + chunk.x() + "," + chunk.z() + " (" + RegradeQueue.size() + " columns)."), false);
        return 1;
    }

    private static void ensureWorld(CommandSourceStack source) {
        if ("none".equals(JobStore.worldId())) {
            try {
                String name = source.getServer().getWorldData().getLevelName();
                JobStore.setWorld(name);
            } catch (Exception ignored) {
                // keep none; save will no-op
            }
        }
    }

    private static String status(int written) {
        String range = Regrade.elevationRange();
        if (written == 0) {
            String f = Regrade.failure();
            return (f.isEmpty() ? "" : " [" + f + "]") + range;
        }
        return range;
    }
}
