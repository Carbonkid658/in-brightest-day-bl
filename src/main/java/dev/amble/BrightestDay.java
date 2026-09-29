package dev.amble;

import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.BrightestDayBlockEntityTypes;
import dev.amble.core.BrightestDayBlocks;
import dev.amble.core.BrightestDayItems;
import dev.amble.core.networking.Networking;
import dev.amble.core.ringpowers.RingPowerRegistry;
import dev.amble.core.ringpowers.RingPowerTicker;
import dev.amble.core.ringpowers.impl.FlightRingPower;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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
		RingPowerRegistry.init();
		BrightestDayAttachments.init();
		RingPowerTicker.init();
		Networking.init();
		FlightRingPower.registerEvents();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
