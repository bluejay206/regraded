package me.bluejay.regraded.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import me.bluejay.regraded.core.Points;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * /rg command tree (Yarn / 1.21.1).
 */
public final class RgCommands {
    private RgCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("rg")
                        .then(CommandManager.literal("import")
                                .then(CommandManager.argument("path", StringArgumentType.greedyString())
                                        .executes(ctx -> importCsv(ctx.getSource(), StringArgumentType.getString(ctx, "path")))))
                        .then(CommandManager.literal("regrade")
                                .then(CommandManager.literal("block")
                                        .executes(ctx -> regradeBlock(ctx.getSource())))
                                .then(CommandManager.literal("chunk")
                                        .executes(ctx -> regradeChunk(ctx.getSource()))))
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
        BlockPos pos = BlockPos.ofFloored(source.getPosition());
        source.sendFeedback(() -> Text.literal("Regrade block/column at " + pos.getX() + "," + pos.getZ() + " queued (" + Points.size() + " points, TIN)."), false);
        return 1;
    }

    private static int regradeChunk(ServerCommandSource source) {
        if (Points.size() == 0) {
            source.sendError(Text.literal("No points loaded. Use /rg import <csv> first."));
            return 0;
        }
        ChunkPos chunk = new ChunkPos(BlockPos.ofFloored(source.getPosition()));
        source.sendFeedback(() -> Text.literal("Regrade chunk " + chunk.x + "," + chunk.z + " queued (" + Points.size() + " points, TIN)."), false);
        return 1;
    }
}
