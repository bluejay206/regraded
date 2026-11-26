package me.bluejay.regraded.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

public class ReGradedClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ReGradedKeybinds.register();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String id;
            if (client.getServer() != null) {
                id = client.getServer().getSaveProperties().getLevelName();
            } else if (client.getCurrentServerEntry() != null) {
                id = client.getCurrentServerEntry().address;
            } else {
                id = "unknown";
            }
            JobStore.enter(id);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> JobStore.leave());
    }
}
