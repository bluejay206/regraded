package me.bluejay.regraded.client;

import net.fabricmc.api.ClientModInitializer;

public class ReGradedClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ReGradedKeybinds.register();
    }
}
