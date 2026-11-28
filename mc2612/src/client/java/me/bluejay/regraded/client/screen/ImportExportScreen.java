package me.bluejay.regraded.client.screen;

import me.bluejay.regraded.JobStore;
import me.bluejay.regraded.core.Points;
import me.bluejay.regraded.core.SurveyPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Import and export the ReGraded point list as PNEZD CSV.
 * Path entry is in-game, confined to the regraded/ folder under the game dir.
 * Also copies points from LevelHeaded's PointLists.STORE when that mod is present.
 */
public final class ImportExportScreen extends Screen {

    private static final long MAX_BYTES = 1_048_576L;

    private final Screen parent;
    private String status = "";

    public ImportExportScreen(Screen parent) {
        super(Component.literal("Import / Export"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = (this.width - 200) / 2;
        int y = this.height / 2 - 50;

        this.addRenderableWidget(Button.builder(Component.literal("Import CSV"), b -> askPath(this::doImport))
                .bounds(x, y, 200, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Export points.csv"), b -> askPath(this::doExport))
                .bounds(x, y + 26, 200, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Import from LevelHeaded"), b -> importFromLevelHeaded())
                .bounds(x, y + 52, 200, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.minecraft.setScreen(parent))
                .bounds(x, y + 88, 200, 20)
                .build());
    }

    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (!status.isEmpty()) {
            graphics.text(this.font, status, this.width / 2 - 140, this.height - 48, 0xFFA0A0A0, false);
        }
    }

    private static Path root() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("regraded").toAbsolutePath().normalize();
    }

    private static Path confine(Path raw) throws IOException {
        Path abs = raw.toAbsolutePath().normalize();
        if (!abs.startsWith(root())) {
            throw new IOException("Path must stay under regraded/");
        }
        return abs;
    }

    private Path defaultPath() {
        return root().resolve("points.csv");
    }

    private void askPath(Consumer<Path> onChosen) {
        this.minecraft.setScreen(new PathScreen(this, onChosen));
    }

    private void doImport(Path path) {
        try {
            Path safe = confine(path);
            if (!Files.isRegularFile(safe)) {
                status = "File not found: " + safe.getFileName();
                return;
            }
            if (Files.size(safe) > MAX_BYTES) {
                status = "File exceeds 1 MB";
                return;
            }
            String csv = Files.readString(safe, StandardCharsets.UTF_8);
            int n = Points.list().importCsv(csv);
            JobStore.save();
            status = "Imported " + n + " points (total " + Points.size() + ")";
        } catch (Exception e) {
            status = "Import failed: " + e.getMessage();
        }
    }

    private void doExport(Path path) {
        try {
            Path target = confine(ensureCsv(path));
            Files.createDirectories(target.getParent());
            Files.writeString(target, Points.list().toCsv(), StandardCharsets.UTF_8);
            JobStore.save();
            status = "Exported " + Points.size() + " points to " + target.getFileName();
        } catch (Exception e) {
            status = "Export failed: " + e.getMessage();
        }
    }

    private Path ensureCsv(Path path) {
        String name = path.getFileName().toString();
        if (name.toLowerCase().endsWith(".csv")) return path;
        return path.resolveSibling(name + ".csv");
    }

    /**
     * Soft copy from LevelHeaded.PointLists.STORE. No hard dependency:
     * the class is loaded by name and the point fields are read by record accessors.
     */
    private void importFromLevelHeaded() {
        try {
            Class<?> lists = Class.forName("me.bluejay.levelheaded.PointLists");
            Object store = lists.getField("STORE").get(null);
            Object list = store.getClass().getMethod("list").invoke(store);
            int added = 0;
            for (Object cp : (Iterable<?>) list) {
                String id = (String) cp.getClass().getMethod("id").invoke(cp);
                double easting = (double) cp.getClass().getMethod("easting").invoke(cp);
                double northing = (double) cp.getClass().getMethod("northing").invoke(cp);
                double elevation = (double) cp.getClass().getMethod("elevation").invoke(cp);
                String desc = (String) cp.getClass().getMethod("description").invoke(cp);
                Points.list().add(new SurveyPoint(id, easting, northing, elevation, desc == null ? "" : desc));
                added++;
            }
            JobStore.save();
            status = "Copied " + added + " points from LevelHeaded";
        } catch (Throwable t) {
            status = "LevelHeaded is not loaded";
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** In-game path entry. Paste a full path or edit the default. */
    private static final class PathScreen extends Screen {
        private final ImportExportScreen parent;
        private final Consumer<Path> onChosen;
        private EditBox field;

        PathScreen(ImportExportScreen parent, Consumer<Path> onChosen) {
            super(Component.literal("CSV Path"));
            this.parent = parent;
            this.onChosen = onChosen;
            parent.status = "";
        }

        @Override
        protected void init() {
            int w = Math.min(360, this.width - 40);
            int x = (this.width - w) / 2;
            int y = this.height / 2 - 10;
            field = new EditBox(this.font, x, y, w, 20, Component.literal("Path"));
            field.setMaxLength(512);
            field.setValue(parent.defaultPath().toString());
            this.addRenderableWidget(field);
            this.setInitialFocus(field);

            this.addRenderableWidget(Button.builder(Component.literal("OK"), b -> confirm())
                    .bounds(x, y + 28, 80, 20)
                    .build());
            this.addRenderableWidget(Button.builder(Component.literal("Cancel"),
                            b -> this.minecraft.setScreen(parent))
                    .bounds(x + 86, y + 28, 80, 20)
                    .build());
        }

        private void confirm() {
            String text = field.getValue().trim();
            if (text.isEmpty()) return;
            try {
                Path safe = confine(Path.of(text));
                onChosen.accept(safe);
                this.minecraft.setScreen(parent);
            } catch (IOException e) {
                parent.status = e.getMessage();
                this.minecraft.setScreen(parent);
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
