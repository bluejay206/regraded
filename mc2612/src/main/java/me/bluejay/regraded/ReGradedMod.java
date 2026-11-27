package me.bluejay.regraded;

import me.bluejay.regraded.bits.BitApplicator;
import me.bluejay.regraded.bits.ChiselsAndBitsApplicator;
import me.bluejay.regraded.command.RgCommands;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReGradedMod implements ModInitializer {
    public static final String MOD_ID = "regraded";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ChiselsAndBitsApplicator cab = new ChiselsAndBitsApplicator();
        if (cab.isAvailable()) {
            BitApplicator.set(cab);
            LOGGER.info("Chisels & Bits detected. Bit applicator enabled.");
        } else {
            BitApplicator.set(BitApplicator.NONE);
            LOGGER.info("Chisels & Bits not present. Regrade will queue only.");
        }
        RgCommands.register();
        LOGGER.info("ReGraded (26.1.2) loaded.");
    }
}
