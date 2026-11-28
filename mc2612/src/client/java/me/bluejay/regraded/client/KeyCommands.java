package me.bluejay.regraded.client;

import com.mojang.blaze3d.platform.InputConstants;
import me.bluejay.regraded.JobStore;
import me.bluejay.regraded.ModItems;
import me.bluejay.regraded.ReGradedMod;
import me.bluejay.regraded.core.Points;
import me.bluejay.regraded.core.SurveyPoint;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Rebindable keys for ReGraded, matching LevelHeaded's key-per-action pattern.
 * Ctrl-Z stays the marker toggle so plain Z remains LevelHeaded's.
 * V (save point) adds a node at the player position when the rod tool is held,
 * matching LevelHeaded's rod capture.
 */
public final class KeyCommands {

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            ReGradedMod.id("assets/regraded")
    );

    public static KeyMapping TOGGLE_MARKERS;
    public static KeyMapping ADD_NODE_PLAYER;

    private static boolean ctrlZWasDown;

    private KeyCommands() {}

    public static void register() {
        TOGGLE_MARKERS = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.regraded.markers",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY
        ));
        ADD_NODE_PLAYER = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.regraded.add_node_player",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.getWindow() == null) return;

            // Ctrl-Z is the established marker toggle (plain Z is LevelHeaded).
            long handle = client.getWindow().handle();
            boolean ctrl = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                    || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
            boolean z = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_Z) == GLFW.GLFW_PRESS;
            boolean down = ctrl && z;
            if (down && !ctrlZWasDown) {
                PointMarkers.toggle();
            }
            ctrlZWasDown = down;

            if (TOGGLE_MARKERS != null) {
                while (TOGGLE_MARKERS.consumeClick()) {
                    PointMarkers.toggle();
                }
            }
            if (ADD_NODE_PLAYER != null) {
                while (ADD_NODE_PLAYER.consumeClick()) {
                    addNodeAtPlayer(client);
                }
            }
            PointMarkers.tick(client);
        });
    }

    /** Add a node at the player's feet, matching LevelHeaded's rod capture. */
    public static void addNodeAtPlayer(Minecraft mc) {
        if (mc.player == null) return;
        var pos = mc.player.position();
        double easting = pos.x;
        double northing = -pos.z;
        double elevation = pos.y;
        String id = "N" + (Points.size() + 1);
        Points.list().add(new SurveyPoint(id, easting, northing, elevation, "rod"));
        JobStore.save();
        mc.player.sendSystemMessage(Component.literal(
                String.format("Added node %s at player %.1f, %.1f, %.1f", id, easting, northing, elevation)));
    }
}
