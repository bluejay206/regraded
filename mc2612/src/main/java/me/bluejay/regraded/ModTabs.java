package me.bluejay.regraded;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModTabs {

    public static final ResourceKey<CreativeModeTab> REGRADED_TAB_KEY =
            ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), ReGradedMod.id("main"));

    public static final CreativeModeTab REGRADED_TAB = FabricCreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.HUB))
            .title(Component.literal("ReGraded"))
            .displayItems((params, output) -> {
                output.accept(ModItems.HUB);
                output.accept(ModItems.REGRADE_TOOL);
                output.accept(ModItems.REGRADE_CHUNK);
                output.accept(ModItems.REMOVE_TOOL);
                output.accept(ModItems.ADD_NODE);
                output.accept(ModItems.DELETE_NODE);
            })
            .build();

    public static void register() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, REGRADED_TAB_KEY, REGRADED_TAB);
        ReGradedMod.LOGGER.info("ReGraded creative tab registered");
    }
}
