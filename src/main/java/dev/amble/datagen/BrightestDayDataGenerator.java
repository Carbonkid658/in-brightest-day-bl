package dev.amble.datagen;

import dev.amble.BrightestDay;
import dev.amble.datagen.providers.BrightestDayLanguageProvider;
import dev.amble.datagen.providers.BrightestDayModelProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

public class BrightestDayDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
		pack.addProvider(BrightestDayLanguageProvider::new);
		pack.addProvider(BrightestDayModelProvider::new);
	}
}
