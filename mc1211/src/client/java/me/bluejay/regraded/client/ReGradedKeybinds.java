package me.bluejay.regraded.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class ReGradedKeybinds {
    public static final String CATEGORY = "key.categories.regraded";
    public static KeyBinding TOGGLE_MARKERS;

    private static boolean rawZWasDown;

    private ReGradedKeybinds() {}

    public static void register() {
        TOGGLE_MARKERS = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.regraded.markers",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                CATEGORY
        ));

        PointMarkers.registerRender();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            // KeyBinding.wasPressed never fires for Z. Use raw Ctrl-Z.
            boolean rawZ = InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_Z);
            boolean ctrl = InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL)
                    || InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL);
            if (rawZ && ctrl && !rawZWasDown) {
                PointMarkers.toggle();
            }
            rawZWasDown = rawZ;
            PointMarkers.tick(client);
        });
    }
}
