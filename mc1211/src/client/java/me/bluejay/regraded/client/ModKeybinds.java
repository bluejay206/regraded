package me.bluejay.regraded.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * ReGraded keybinds (Yarn). Visualization toggle is Ctrl-Z.
 */
public final class ModKeybinds {
    public static KeyBinding TOGGLE_MARKERS;

    private ModKeybinds() {}

    public static void register() {
        TOGGLE_MARKERS = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.regraded.toggle_markers",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "key.categories.regraded"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (TOGGLE_MARKERS != null && TOGGLE_MARKERS.wasPressed()) {
                long window = MinecraftClient.getInstance().getWindow().getHandle();
                boolean ctrl = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                        || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
                if (ctrl) {
                    PointMarkers.toggle();
                }
            }
            PointMarkers.tick(client);
        });
    }
}
