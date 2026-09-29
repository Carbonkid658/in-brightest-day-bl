package dev.amble;

import dev.amble.core.BrightestDayBlockEntityTypes;
import dev.amble.core.BrightestDayBlocks;
import dev.amble.core.BrightestDayItems;
import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrightestDay implements ModInitializer {
	public static final String MOD_ID = "brightestday";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		BrightestDayItems.init();
		BrightestDayBlocks.init();
		BrightestDayBlockEntityTypes.init();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
