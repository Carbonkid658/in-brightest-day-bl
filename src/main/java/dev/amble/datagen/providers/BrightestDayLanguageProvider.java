package dev.amble.datagen.providers;

import dev.amble.core.BrightestDayBlocks;
import dev.amble.core.BrightestDayItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;

public class BrightestDayLanguageProvider extends FabricLanguageProvider {
    public BrightestDayLanguageProvider(FabricPackOutput packOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(packOutput, registryLookup);
    }

    @Override
    public void generateTranslations(HolderLookup.Provider registryLookup, TranslationBuilder builder) {
        builder.add(BrightestDayItems.POWER_RING, "Green Power Ring");
        builder.add(BrightestDayBlocks.GREEN_LANTERN_BLOCK, "Green Lantern");
    }
}
