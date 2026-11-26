package me.bluejay.regraded.client;

import me.bluejay.regraded.core.Points;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Point list, one folder per world. Mirrors LevelHeaded's JobStore.
 */
public final class JobStore {

    private static String worldId = "none";

    private JobStore() {}

    public static void enter(String id) {
        worldId = sanitize(id);
        Points.clear();
        load();
    }

    public static void leave() {
        save();
        Points.clear();
        worldId = "none";
    }

    private static String sanitize(String id) {
        if (id == null || id.isBlank()) return "unknown";
        return id.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static Path dir() {
        return MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("regraded").resolve("worlds").resolve(worldId);
    }

    private static Path pointsFile() { return dir().resolve("points.csv"); }

    public static void load() {
        try {
            Path points = pointsFile();
            if (Files.exists(points)) {
                Points.list().importCsv(Files.readString(points, StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {
            // start empty
        }
    }

    public static void save() {
        if ("none".equals(worldId)) return;
        try {
            Files.createDirectories(dir());
            Files.writeString(pointsFile(), Points.list().toCsv(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // keep the in-memory state
        }
    }
}
