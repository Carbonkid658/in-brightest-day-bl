package dev.amble.datagen.providers;

import dev.amble.core.BrightestDayBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;

public class BrightestDayLootTableProvider extends FabricBlockLootSubProvider {

    public BrightestDayLootTableProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookupFuture) {
        super(output, registryLookupFuture);
    }

    @Override
    public void generate() {
        this.dropSelf(BrightestDayBlocks.GREEN_LANTERN_BLOCK);
        this.dropSelf(BrightestDayBlocks.YELLOW_LANTERN_BLOCK);
        this.dropSelf(BrightestDayBlocks.BLACK_LANTERN_BLOCK);
        this.dropSelf(BrightestDayBlocks.WHITE_LANTERN_BLOCK);
        this.dropSelf(BrightestDayBlocks.RED_LANTERN_BLOCK);
        this.dropSelf(BrightestDayBlocks.ORANGE_LANTERN_BLOCK);
        this.dropSelf(BrightestDayBlocks.BLUE_LANTERN_BLOCK);
        this.dropSelf(BrightestDayBlocks.INDIGO_LANTERN_BLOCK);
        this.dropSelf(BrightestDayBlocks.STAR_SAPPHIRE_LANTERN_BLOCK);
    }
}
