package me.bluejay.regraded.client;

import me.bluejay.regraded.JobStore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReGradedClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("regraded");
    private static boolean ctrlZWasDown = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("ReGraded client initialized (26.1.2)");

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String id = worldId(client);
            JobStore.enter(id);
            LOGGER.info("ReGraded loaded {} points for world {}", me.bluejay.regraded.core.Points.size(), id);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> JobStore.leave());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getWindow() == null) return;
            long handle = client.getWindow().handle();
            boolean ctrl = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                    || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
            boolean z = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_Z) == GLFW.GLFW_PRESS;
            boolean down = ctrl && z;
            if (down && !ctrlZWasDown) {
                PointMarkers.toggle();
            }
            ctrlZWasDown = down;
            PointMarkers.tick(client);
        });
    }

    private static String worldId(Minecraft client) {
        try {
            if (client.getSingleplayerServer() != null) {
                return client.getSingleplayerServer().getWorldData().getLevelName();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "default";
    }
}
