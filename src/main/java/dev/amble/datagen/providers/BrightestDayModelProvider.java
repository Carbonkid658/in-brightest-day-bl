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
        itemModelGenerators.generateSpyglass(BrightestDayItems.POWER_RING);
        itemModelGenerators.generateSpyglass(BrightestDayBlocks.GREEN_LANTERN_BLOCK.asItem());
        itemModelGenerators.generateSpyglass(BrightestDayBlocks.YELLOW_LANTERN_BLOCK.asItem());
    }
}
