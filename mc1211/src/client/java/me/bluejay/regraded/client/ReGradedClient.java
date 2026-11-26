package me.bluejay.regraded.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReGradedClient implements ClientModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("regraded");

    @Override
    public void onInitializeClient() {
        LOG.info("ReGraded client initializing");
        ReGradedKeybinds.register();
        LOG.info("ReGraded client ready");
    }
}
