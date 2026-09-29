package dev.amble.core;

import dev.amble.BrightestDay;
import dev.amble.core.blocks.GreenLanternBlock;
import net.minecraft.core.Registry;
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
    public static final Block GREEN_LANTERN_BLOCK = registerBlock("green_lantern", properties -> new GreenLanternBlock(properties.lightLevel(_ -> 7)));

    private static <T extends Block> T registerBlock(String name, Function<BlockBehaviour.Properties, T> function) {
        Identifier id = BrightestDay.id(name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);

        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().setId(blockKey);
        T block = function.apply(properties);

        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        registerBlockItem(name, block);
        return block;
    }

    private static <T extends Block> void registerBlockItem(String name, T block) {
        Identifier id = BrightestDay.id(name);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);

        Item.Properties properties = new Item.Properties()
                .useBlockDescriptionPrefix()
                .setId(itemKey);

        Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, properties));
    }

    public static void init() {}
}
