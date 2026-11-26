package me.bluejay.regraded;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReGraded implements ModInitializer {
    public static final String MOD_ID = "regraded";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("ReGraded (26.3) loaded.");
    }
}
