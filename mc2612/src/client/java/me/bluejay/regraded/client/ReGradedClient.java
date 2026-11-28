package me.bluejay.regraded.client;

import me.bluejay.regraded.JobStore;
import me.bluejay.regraded.ModItems;
import me.bluejay.regraded.client.screen.HubScreen;
import me.bluejay.regraded.core.Points;
import me.bluejay.regraded.core.SurveyPoint;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReGradedClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("regraded");
    private static boolean ctrlZWasDown = false;
    private static final double RAY_RANGE = 100.0;
    private static final double NODE_PICK_RANGE = 1.0;

    @Override
    public void onInitializeClient() {
        LOGGER.info("ReGraded client initialized (26.1.2)");
        PointMarkers.registerRender();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String id = worldId(client);
            JobStore.enter(id);
            LOGGER.info("ReGraded loaded {} points for world {}", Points.size(), id);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> JobStore.leave());

        // Client-only tools: hub menu, add node, delete node.
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClientSide()) return InteractionResult.PASS;
            var stack = player.getItemInHand(hand);
            Minecraft mc = Minecraft.getInstance();

            if (stack.is(ModItems.HUB)) {
                mc.setScreen(new HubScreen());
                return InteractionResult.SUCCESS;
            }
            if (stack.is(ModItems.ADD_NODE)) {
                return addNode(mc);
            }
            if (stack.is(ModItems.DELETE_NODE)) {
                return deleteNode(mc);
            }
            return InteractionResult.PASS;
        });

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
            updateDeleteHighlight(client);
        });
    }

    /** While the delete tool is held, highlight the nearest node within 1 block of the crosshair. */
    private static void updateDeleteHighlight(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            PointMarkers.setHighlight(-1);
            return;
        }
        if (!mc.player.getMainHandItem().is(ModItems.DELETE_NODE)
                && !mc.player.getOffhandItem().is(ModItems.DELETE_NODE)) {
            PointMarkers.setHighlight(-1);
            return;
        }
        HitResult hit = mc.player.pick(RAY_RANGE, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            PointMarkers.setHighlight(-1);
            return;
        }
        var pos = ((BlockHitResult) hit).getBlockPos();
        double easting = pos.getX() + 0.5;
        double northing = -(pos.getZ() + 0.5);
        int idx = Points.list().nearestIndex(easting, northing, NODE_PICK_RANGE);
        PointMarkers.setHighlight(idx);
    }

    private static InteractionResult addNode(Minecraft mc) {
        if (mc.player == null || mc.level == null) return InteractionResult.PASS;
        HitResult hit = mc.player.pick(RAY_RANGE, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            mc.player.sendSystemMessage(Component.literal("No block in look direction."));
            return InteractionResult.SUCCESS;
        }
        var pos = ((BlockHitResult) hit).getBlockPos();
        // world X = easting, world Z = -northing
        double easting = pos.getX() + 0.5;
        double northing = -(pos.getZ() + 0.5);
        double elevation = pos.getY() + 1.0;
        String id = "N" + (Points.size() + 1);
        Points.list().add(new SurveyPoint(id, easting, northing, elevation, "node"));
        JobStore.save();
        mc.player.sendSystemMessage(Component.literal(
                String.format("Added node %s at %.1f, %.1f, %.1f", id, easting, northing, elevation)));
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult deleteNode(Minecraft mc) {
        if (mc.player == null || mc.level == null) return InteractionResult.PASS;
        HitResult hit = mc.player.pick(RAY_RANGE, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            mc.player.sendSystemMessage(Component.literal("No block in look direction."));
            return InteractionResult.SUCCESS;
        }
        var pos = ((BlockHitResult) hit).getBlockPos();
        double easting = pos.getX() + 0.5;
        double northing = -(pos.getZ() + 0.5);
        boolean removed = Points.list().removeNear(easting, northing, NODE_PICK_RANGE);
        if (removed) {
            JobStore.save();
            mc.player.sendSystemMessage(Component.literal("Removed nearest node."));
        } else {
            mc.player.sendSystemMessage(Component.literal("No node within 1 block."));
        }
        return InteractionResult.SUCCESS;
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
