package me.bluejay.regraded;

import me.bluejay.regraded.bits.BitApplicator;
import me.bluejay.regraded.bits.BitsAndChiselsApplicator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReGraded implements ModInitializer {
    public static final String MOD_ID = "regraded";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        boolean hasBC = FabricLoader.getInstance().isModLoaded("bitsandchisels");
        if (hasBC) {
            BitApplicator.Holder.INSTANCE = new BitsAndChiselsApplicator();
            LOGGER.info("ReGraded: BitsAndChisels detected — bit surface applicator enabled");
        } else {
            LOGGER.warn("ReGraded: BitsAndChisels not found — regrading disabled");
        }
        LOGGER.info("ReGraded (26.3) loaded. Bit applicator available: {}", BitApplicator.get().isAvailable());
    }
}
