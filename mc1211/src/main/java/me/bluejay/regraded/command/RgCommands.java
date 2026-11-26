package me.bluejay.regraded.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import me.bluejay.regraded.core.Points;
import me.bluejay.regraded.regrade.Regrade;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * /rg command tree (Yarn / 1.21.1).
 */
public final class RgCommands {
    private static final double RAY_RANGE = 100.0;

    private RgCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("rg")
                        .then(CommandManager.literal("import")
                                .then(CommandManager.argument("path", StringArgumentType.greedyString())
                                        .executes(ctx -> importCsv(ctx.getSource(), StringArgumentType.getString(ctx, "path")))))
                        .then(CommandManager.literal("regrade")
                                .then(CommandManager.literal("block")
                                        .executes(ctx -> regradeBlock(ctx.getSource()))
                                        .then(CommandManager.literal("raycast")
                                                .executes(ctx -> regradeBlockRaycast(ctx.getSource()))))
                                .then(CommandManager.literal("chunk")
                                        .executes(ctx -> regradeChunk(ctx.getSource()))
                                        .then(CommandManager.literal("raycast")
                                                .executes(ctx -> regradeChunkRaycast(ctx.getSource())))))
                        .then(CommandManager.literal("clear")
                                .executes(ctx -> {
                                    Points.clear();
                                    ctx.getSource().sendFeedback(() -> Text.literal("Point list cleared."), false);
                                    return 1;
                                }))
                        .then(CommandManager.literal("count")
                                .executes(ctx -> {
                                    ctx.getSource().sendFeedback(() -> Text.literal("Points: " + Points.size()), false);
                                    return 1;
                                }))
                ));
    }

    private static int importCsv(ServerCommandSource source, String pathStr) {
        try {
            Path path = Path.of(pathStr);
            if (!Files.exists(path)) {
                source.sendError(Text.literal("File not found: " + pathStr));
                return 0;
            }
            String csv = Files.readString(path);
            int added = Points.list().importCsv(csv);
            source.sendFeedback(() -> Text.literal("Imported " + added + " points (total " + Points.size() + ") from " + path.getFileName()), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Import failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int regradeBlock(ServerCommandSource source) {
        if (Points.size() == 0) {
            source.sendError(Text.literal("No points loaded. Use /rg import <csv> first."));
            return 0;
        }
        ServerWorld world = source.getWorld();
        BlockPos pos = BlockPos.ofFloored(source.getPosition());
        int written = Regrade.block(world, pos);
        source.sendFeedback(() -> Text.literal("Regraded block/column at " + pos.getX() + "," + pos.getZ() + " (" + written + " bits, TIN)."), false);
        return 1;
    }

    private static int regradeBlockRaycast(ServerCommandSource source) {
        if (Points.size() == 0) {
            source.sendError(Text.literal("No points loaded. Use /rg import <csv> first."));
            return 0;
        }
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Raycast requires a player."));
            return 0;
        }
        HitResult hit = player.raycast(RAY_RANGE, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            source.sendError(Text.literal("No block in look direction."));
            return 0;
        }
        ServerWorld world = source.getWorld();
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        int written = Regrade.block(world, pos);
        source.sendFeedback(() -> Text.literal("Regraded block under raycast at " + pos.getX() + "," + pos.getY() + "," + pos.getZ() + " (" + written + " bits, TIN)."), false);
        return 1;
    }

    private static int regradeChunk(ServerCommandSource source) {
        if (Points.size() == 0) {
            source.sendError(Text.literal("No points loaded. Use /rg import <csv> first."));
            return 0;
        }
        ServerWorld world = source.getWorld();
        ChunkPos chunk = new ChunkPos(BlockPos.ofFloored(source.getPosition()));
        int written = Regrade.chunk(world, chunk);
        source.sendFeedback(() -> Text.literal("Regraded chunk " + chunk.x + "," + chunk.z + " (" + written + " bits, TIN)."), false);
        return 1;
    }

    private static int regradeChunkRaycast(ServerCommandSource source) {
        if (Points.size() == 0) {
            source.sendError(Text.literal("No points loaded. Use /rg import <csv> first."));
            return 0;
        }
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Raycast requires a player."));
            return 0;
        }
        HitResult hit = player.raycast(RAY_RANGE, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            source.sendError(Text.literal("No block in look direction."));
            return 0;
        }
        ServerWorld world = source.getWorld();
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        ChunkPos chunk = new ChunkPos(pos);
        int written = Regrade.chunk(world, chunk);
        source.sendFeedback(() -> Text.literal("Regraded chunk under raycast " + chunk.x + "," + chunk.z + " (" + written + " bits, TIN)."), false);
        return 1;
    }
}
