package me.bluejay.regraded.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * ReGraded keybinds. Visualization toggle is Ctrl-Z.
 */
public final class ModKeybinds {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            "key.categories.regraded"
    );

    public static KeyMapping TOGGLE_MARKERS;

    private ModKeybinds() {}

    public static void register() {
        TOGGLE_MARKERS = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.regraded.toggle_markers",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (TOGGLE_MARKERS != null && TOGGLE_MARKERS.consumeClick()) {
                long window = Minecraft.getInstance().getWindow().getWindow();
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
