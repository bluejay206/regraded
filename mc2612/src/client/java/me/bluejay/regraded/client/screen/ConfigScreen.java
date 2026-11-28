package me.bluejay.regraded.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Placeholder configuration screen. Settings will land here later. */
public final class ConfigScreen extends Screen {

    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.literal("ReGraded Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = (this.width - 200) / 2;
        int y = this.height / 2 - 20;

        this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.minecraft.setScreen(parent))
                .bounds(x, y, 200, 20)
                .build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
