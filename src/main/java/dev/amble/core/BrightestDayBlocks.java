package dev.amble.core;

import dev.amble.BrightestDay;
import dev.amble.core.blocks.HardLightBlock;
import dev.amble.core.blocks.LanternBlock;
import dev.amble.core.ringpowers.LanternCorps;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

public class BrightestDayBlocks {


    public static final Block GREEN_LANTERN_BLOCK = registerBlockWithItem("green_lantern",
            properties -> new LanternBlock(LanternCorps.GREEN, properties.lightLevel(_ -> 12).mapColor(MapColor.METAL).forceSolidOn().strength(3.5F)
                    .sound(SoundType.LANTERN).noOcclusion().pushReaction(PushReaction.POPPED)),
            new Item.Properties().component(DataComponents.TOOLTIP_STYLE, BrightestDay.id("ring")));

    public static final Block YELLOW_LANTERN_BLOCK = registerBlockWithItem("yellow_lantern",
            properties -> new LanternBlock(LanternCorps.YELLOW, properties.lightLevel(_ -> 12).mapColor(MapColor.METAL).forceSolidOn().strength(3.5F)
                    .sound(SoundType.LANTERN).noOcclusion().pushReaction(PushReaction.POPPED)),
            new Item.Properties().component(DataComponents.TOOLTIP_STYLE, BrightestDay.id("ring")));

    public static final Block BLACK_LANTERN_BLOCK = registerBlockWithItem("black_lantern",
            properties -> new LanternBlock(LanternCorps.BLACK, properties.lightLevel(_ -> 12).mapColor(MapColor.METAL).forceSolidOn().strength(3.5F)
                    .sound(SoundType.LANTERN).noOcclusion().pushReaction(PushReaction.POPPED)),
            new Item.Properties().component(DataComponents.TOOLTIP_STYLE, BrightestDay.id("ring")));

    public static final Block WHITE_LANTERN_BLOCK = registerBlockWithItem("white_lantern",
            properties -> new LanternBlock(LanternCorps.WHITE, properties.lightLevel(_ -> 12).mapColor(MapColor.METAL).forceSolidOn().strength(3.5F)
                    .sound(SoundType.LANTERN).noOcclusion().pushReaction(PushReaction.POPPED)),
            new Item.Properties().component(DataComponents.TOOLTIP_STYLE, BrightestDay.id("ring")));

    public static final Block RED_LANTERN_BLOCK = registerBlockWithItem("red_lantern",
            properties -> new LanternBlock(LanternCorps.RED, properties.lightLevel(_ -> 12).mapColor(MapColor.METAL).forceSolidOn().strength(3.5F)
                    .sound(SoundType.LANTERN).noOcclusion().pushReaction(PushReaction.POPPED)),
            new Item.Properties().component(DataComponents.TOOLTIP_STYLE, BrightestDay.id("ring")));

    public static final Block ORANGE_LANTERN_BLOCK = registerBlockWithItem("orange_lantern",
            properties -> new LanternBlock(LanternCorps.ORANGE, properties.lightLevel(_ -> 12).mapColor(MapColor.METAL).forceSolidOn().strength(3.5F)
                    .sound(SoundType.LANTERN).noOcclusion().pushReaction(PushReaction.POPPED)),
            new Item.Properties().component(DataComponents.TOOLTIP_STYLE, BrightestDay.id("ring")));

    public static final Block BLUE_LANTERN_BLOCK = registerBlockWithItem("blue_lantern",
            properties -> new LanternBlock(LanternCorps.BLUE, properties.lightLevel(_ -> 12).mapColor(MapColor.METAL).forceSolidOn().strength(3.5F)
                    .sound(SoundType.LANTERN).noOcclusion().pushReaction(PushReaction.POPPED)),
            new Item.Properties().component(DataComponents.TOOLTIP_STYLE, BrightestDay.id("ring")));

    public static final Block INDIGO_LANTERN_BLOCK = registerBlockWithItem("indigo_lantern",
            properties -> new LanternBlock(LanternCorps.INDIGO, properties.lightLevel(_ -> 12).mapColor(MapColor.METAL).forceSolidOn().strength(3.5F)
                    .sound(SoundType.LANTERN).noOcclusion().pushReaction(PushReaction.POPPED)),
            new Item.Properties().component(DataComponents.TOOLTIP_STYLE, BrightestDay.id("ring")));

    public static final Block STAR_SAPPHIRE_LANTERN_BLOCK = registerBlockWithItem("sapphire_lantern",
            properties -> new LanternBlock(LanternCorps.STAR_SAPPHIRE, properties.lightLevel(_ -> 12).mapColor(MapColor.METAL).forceSolidOn().strength(3.5F)
                    .sound(SoundType.LANTERN).noOcclusion().pushReaction(PushReaction.POPPED)),
            new Item.Properties().component(DataComponents.TOOLTIP_STYLE, BrightestDay.id("ring")));

    public static final Block HARD_LIGHT = registerBlock("hard_light",
            properties -> new HardLightBlock(properties.strength(-1.0F, 3600000.0F).noLootTable().noOcclusion()
                    .lightLevel(_ -> 8).sound(SoundType.AMETHYST).pushReaction(PushReaction.IMMOVEABLE)
                    .isValidSpawn((state, level, pos, type) -> false).isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos, box) -> false)));

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
