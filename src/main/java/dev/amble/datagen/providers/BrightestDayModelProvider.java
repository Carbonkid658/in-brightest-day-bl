package dev.amble.datagen.providers;

import dev.amble.core.BrightestDayBlocks;
import dev.amble.core.BrightestDayItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TexturedModel;

public class BrightestDayModelProvider extends FabricModelProvider {
    public BrightestDayModelProvider(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockModelGenerators) {}

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerators) {
        itemModelGenerators.generateSpyglass(BrightestDayItems.GREEN_POWER_RING);
        itemModelGenerators.generateSpyglass(BrightestDayItems.YELLOW_POWER_RING);
        itemModelGenerators.generateSpyglass(BrightestDayItems.ORANGE_POWER_RING);
        itemModelGenerators.generateSpyglass(BrightestDayItems.RED_POWER_RING);
        itemModelGenerators.generateSpyglass(BrightestDayItems.INDIGO_POWER_RING);
        itemModelGenerators.generateSpyglass(BrightestDayItems.STAR_SAPPHIRE_POWER_RING);
        itemModelGenerators.generateSpyglass(BrightestDayItems.BLUE_POWER_RING);
        itemModelGenerators.generateSpyglass(BrightestDayItems.WHITE_POWER_RING);
        itemModelGenerators.generateSpyglass(BrightestDayItems.BLACK_POWER_RING);
        itemModelGenerators.generateSpyglass(BrightestDayBlocks.GREEN_LANTERN_BLOCK.asItem());
        itemModelGenerators.generateSpyglass(BrightestDayBlocks.YELLOW_LANTERN_BLOCK.asItem());
        itemModelGenerators.generateSpyglass(BrightestDayBlocks.BLACK_LANTERN_BLOCK.asItem());
        itemModelGenerators.generateSpyglass(BrightestDayBlocks.WHITE_LANTERN_BLOCK.asItem());
        itemModelGenerators.generateSpyglass(BrightestDayBlocks.RED_LANTERN_BLOCK.asItem());
        itemModelGenerators.generateSpyglass(BrightestDayBlocks.ORANGE_LANTERN_BLOCK.asItem());
        itemModelGenerators.generateSpyglass(BrightestDayBlocks.BLUE_LANTERN_BLOCK.asItem());
        itemModelGenerators.generateSpyglass(BrightestDayBlocks.INDIGO_LANTERN_BLOCK.asItem());
        itemModelGenerators.generateSpyglass(BrightestDayBlocks.STAR_SAPPHIRE_LANTERN_BLOCK.asItem());
    }
}
