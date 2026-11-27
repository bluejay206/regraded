package me.bluejay.regraded.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReGradedClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("regraded");
    private static boolean markersVisible = false;
    private static boolean ctrlZWasDown = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("ReGraded client initialized (26.1.2)");
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getWindow() == null) return;
            long handle = client.getWindow().getWindow();
            boolean ctrl = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                    || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
            boolean z = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_Z) == GLFW.GLFW_PRESS;
            boolean down = ctrl && z;
            if (down && !ctrlZWasDown) {
                markersVisible = !markersVisible;
                LOGGER.info("ReGraded markers {}", markersVisible ? "ON" : "OFF");
                // TODO: PointMarkers.toggle() once ported
            }
            ctrlZWasDown = down;
        });
    }
}
