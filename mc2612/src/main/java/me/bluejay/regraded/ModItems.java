package me.bluejay.regraded;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * ReGraded tools. Models parent to vanilla items (hoe, TNT, shovel, end rod, sponge).
 * Right-click behavior is wired in ReGradedClient (client) and ReGradedMod (server).
 */
public final class ModItems {

    public static final Item REGRADE_TOOL = register("regrade_tool", new Item(new Item.Properties()
            .setId(key("regrade_tool"))
            .stacksTo(1)));

    public static final Item REGRADE_CHUNK = register("regrade_chunk", new Item(new Item.Properties()
            .setId(key("regrade_chunk"))
            .stacksTo(1)));

    public static final Item REMOVE_TOOL = register("remove_tool", new Item(new Item.Properties()
            .setId(key("remove_tool"))
            .stacksTo(1)));

    public static final Item REMOVE_CHUNK = register("remove_chunk", new Item(new Item.Properties()
            .setId(key("remove_chunk"))
            .stacksTo(1)));

    public static final Item ADD_NODE = register("add_node", new Item(new Item.Properties()
            .setId(key("add_node"))
            .stacksTo(1)));

    /** Adds a node at the player's position, matching LevelHeaded's rod. */
    public static final Item ADD_NODE_PLAYER = register("add_node_player", new Item(new Item.Properties()
            .setId(key("add_node_player"))
            .stacksTo(1)));

    public static final Item DELETE_NODE = register("delete_node", new Item(new Item.Properties()
            .setId(key("delete_node"))
            .stacksTo(1)));

    public static final Item HUB = register("hub", new Item(new Item.Properties()
            .setId(key("hub"))
            .stacksTo(1)));

    private static Item register(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, ReGradedMod.id(name), item);
    }

    private static ResourceKey<Item> key(String name) {
        return ResourceKey.create(Registries.ITEM, ReGradedMod.id(name));
    }

    public static void register() {
        // Static fields force registration on class load.
        ReGradedMod.LOGGER.info("ReGraded items registered");
    }
}
