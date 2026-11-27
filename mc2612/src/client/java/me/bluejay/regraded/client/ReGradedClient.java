package me.bluejay.regraded.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReGradedClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("regraded");

    @Override
    public void onInitializeClient() {
        LOGGER.info("ReGraded client initialized (26.1.2)");
    }
}
