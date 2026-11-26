package me.bluejay.regraded.client;

import me.bluejay.regraded.core.Points;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class ReGradedKeybinds {
    public static final String CATEGORY = "key.categories.regraded";
    public static KeyBinding TOGGLE_MARKERS;

    private ReGradedKeybinds() {}

    public static void register() {
        TOGGLE_MARKERS = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.regraded.markers",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (TOGGLE_MARKERS != null && isCtrlDown(client) && TOGGLE_MARKERS.wasPressed()) {
                PointMarkers.toggle();
            }
            PointMarkers.tick(client);
        });
    }

    private static boolean isCtrlDown(MinecraftClient client) {
        long handle = client.getWindow().getHandle();
        return InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }
}
