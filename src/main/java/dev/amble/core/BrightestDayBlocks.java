package dev.amble.core;

import dev.amble.BrightestDay;
import dev.amble.core.blocks.GreenLanternBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public class BrightestDayBlocks {
    public static final Block GREEN_LANTERN_BLOCK = registerBlockWithItem("green_lantern",
            properties -> new GreenLanternBlock(properties.lightLevel(_ -> 7)),
            new Item.Properties().component(DataComponents.TOOLTIP_STYLE, BrightestDay.id("ring")));

    private static <T extends Block> T registerBlockWithItem(String name, Function<BlockBehaviour.Properties, T> function, Item.Properties itemProperties) {
        T block = registerBlock(name, function);

        registerBlockItem(name, block, itemProperties);

        return block;
    }

    private static <T extends Block> T registerBlock(String name, Function<BlockBehaviour.Properties, T> function) {
        Identifier id = BrightestDay.id(name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);

        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().setId(blockKey);
        T block = function.apply(properties);

        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        return block;
    }

    private static <T extends Block> void registerBlockItem(String name, T block, Item.Properties properties) {
        Identifier id = BrightestDay.id(name);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);

        Item.Properties props = properties.setId(itemKey).useBlockDescriptionPrefix();

        Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, props));
    }

    public static void init() {}
}
