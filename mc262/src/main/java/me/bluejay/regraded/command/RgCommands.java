package me.bluejay.regraded.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import me.bluejay.regraded.core.Points;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * /rg command tree (Mojang / 26.2).
 */
public final class RgCommands {
    private RgCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("rg")
                        .then(Commands.literal("import")
                                .then(Commands.argument("path", StringArgumentType.greedyString())
                                        .executes(ctx -> importCsv(ctx.getSource(), StringArgumentType.getString(ctx, "path")))))
                        .then(Commands.literal("regrade")
                                .then(Commands.literal("block")
                                        .executes(ctx -> regradeBlock(ctx.getSource())))
                                .then(Commands.literal("chunk")
                                        .executes(ctx -> regradeChunk(ctx.getSource()))))
                        .then(Commands.literal("clear")
                                .executes(ctx -> {
                                    Points.clear();
                                    ctx.getSource().sendSuccess(() -> Component.literal("Point list cleared."), false);
                                    return 1;
                                }))
                        .then(Commands.literal("count")
                                .executes(ctx -> {
                                    ctx.getSource().sendSuccess(() -> Component.literal("Points: " + Points.size()), false);
                                    return 1;
                                }))
                ));
    }

    private static int importCsv(CommandSourceStack source, String pathStr) {
        try {
            Path path = Path.of(pathStr);
            if (!Files.exists(path)) {
                source.sendFailure(Component.literal("File not found: " + pathStr));
                return 0;
            }
            String csv = Files.readString(path);
            int added = Points.list().importCsv(csv);
            source.sendSuccess(() -> Component.literal("Imported " + added + " points (total " + Points.size() + ") from " + path.getFileName()), false);
            return 1;
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
        BlockPos pos = BlockPos.containing(source.getPosition());
        source.sendSuccess(() -> Component.literal("Regrade block/column at " + pos.getX() + "," + pos.getZ() + " queued (" + Points.size() + " points, TIN)."), false);
        return 1;
    }

    private static int regradeChunk(CommandSourceStack source) {
        if (Points.size() == 0) {
            source.sendFailure(Component.literal("No points loaded. Use /rg import <csv> first."));
            return 0;
        }
        ChunkPos chunk = new ChunkPos(BlockPos.containing(source.getPosition()));
        source.sendSuccess(() -> Component.literal("Regrade chunk " + chunk.x + "," + chunk.z + " queued (" + Points.size() + " points, TIN)."), false);
        return 1;
    }
}
