package dev.amble.datagen.providers;

import dev.amble.core.BrightestDayBlocks;
import dev.amble.core.BrightestDayItems;
import dev.amble.core.ringpowers.LanternCorps;
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

        builder.add("container.brightestday.lantern", "Lantern");
        builder.add("gui.brightestday.lantern", "Lantern");
        builder.add("gui.brightestday.inventory", "Inventory");
        builder.add("gui.brightestday.no_ring", "No ring equipped");

        builder.add(LanternCorps.GREEN.getTranslationKey(), "Green Lantern Corps");
        builder.add(LanternCorps.BLUE.getTranslationKey(), "Blue Lantern Corps");
        builder.add(LanternCorps.YELLOW.getTranslationKey(), "Sinestro Corps");
        builder.add(LanternCorps.ORANGE.getTranslationKey(), "Orange Lanterns");
        builder.add(LanternCorps.RED.getTranslationKey(), "Red Lantern Corps");
        builder.add(LanternCorps.INDIGO.getTranslationKey(), "Indigo Tribe");
        builder.add(LanternCorps.STAR_SAPPHIRE.getTranslationKey(), "Star Sapphires");
        builder.add(LanternCorps.WHITE.getTranslationKey(), "White Lantern Corps");
        builder.add(LanternCorps.BLACK.getTranslationKey(), "Black Lantern Corps");

        builder.add("key.category.brightestday.main", "In Brightest Day");
        builder.add("key.brightestday.power_1", "Ring Power 1");
        builder.add("key.brightestday.power_2", "Ring Power 2");
        builder.add("key.brightestday.power_3", "Ring Power 3");
        builder.add("key.brightestday.power_4", "Ring Power 4");
    }
}
