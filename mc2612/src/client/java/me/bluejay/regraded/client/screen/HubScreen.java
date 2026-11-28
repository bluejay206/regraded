package me.bluejay.regraded.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * ReGraded hub: configuration, point list, import/export, and a bridge
 * to the LevelHeaded point list when that mod is present.
 */
public final class HubScreen extends Screen {

    private static final int BTN_W = 200;
    private static final int BTN_H = 20;
    private static final int GAP = 6;

    public HubScreen() {
        super(Component.literal("ReGraded"));
    }

    @Override
    protected void init() {
        int x = (this.width - BTN_W) / 2;
        int y = this.height / 2 - 60;

        add(x, y, "Configuration", () -> this.minecraft.setScreen(new ConfigScreen(this)));
        add(x, y + (BTN_H + GAP), "Point List", () -> this.minecraft.setScreen(new PointsScreen(this)));
        add(x, y + 2 * (BTN_H + GAP), "Import / Export", () -> this.minecraft.setScreen(new ImportExportScreen(this)));
        add(x, y + 3 * (BTN_H + GAP), "LevelHeaded Points", this::openLevelHeaded);

        this.addRenderableWidget(Button.builder(Component.literal("Close"), b -> this.onClose())
                .bounds(x, y + 4 * (BTN_H + GAP) + 8, BTN_W, BTN_H)
                .build());
    }

    private void openLevelHeaded() {
        // Soft bridge: if LevelHeaded is loaded, open its data collector.
        // Otherwise show a message. The actual screen class is referenced
        // by name so this compiles without a hard dependency.
        try {
            Class<?> cls = Class.forName("me.bluejay.levelheaded.client.screen.DataCollectorScreen");
            Screen screen = (Screen) cls.getDeclaredConstructor().newInstance();
            this.minecraft.setScreen(screen);
        } catch (Throwable t) {
            if (this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(
                        Component.literal("LevelHeaded is not loaded."));
            }
        }
    }

    private void add(int x, int y, String label, Runnable action) {
        this.addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y, BTN_W, BTN_H)
                .build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
