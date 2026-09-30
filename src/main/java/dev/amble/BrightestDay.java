package dev.amble;

import dev.amble.config.BrightestDayConfig;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.BrightestDayBlockEntityTypes;
import dev.amble.core.BrightestDayBlocks;
import dev.amble.core.BrightestDayCreativeTabs;
import dev.amble.core.BrightestDayItems;
import dev.amble.core.BrightestDayMenus;
import dev.amble.core.BrightestDaySounds;
import dev.amble.core.networking.Networking;
import dev.amble.core.ringpowers.RingPowerRegistry;
import dev.amble.core.ringpowers.RingPowerTicker;
import dev.amble.core.ringpowers.impl.FlightRingPower;
import dev.amble.core.shields.ShieldManager;
import dev.amble.core.tractor.TractorManager;
import dev.amble.core.ringpowers.constructs.ConstructTools;
import dev.amble.core.walls.WallManager;
import dev.amble.core.sculpt.SculptManager;
import dev.amble.core.ringpowers.RingJumpstart;
import dev.amble.core.beams.BeamManager;
import dev.amble.core.beams.HealBeamManager;
import dev.amble.core.ringpowers.CorpsSynergy;
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
		BrightestDayConfig.load();
		BrightestDayItems.init();
		BrightestDayBlocks.init();
		BrightestDayBlockEntityTypes.init();
		BrightestDayMenus.init();
		BrightestDaySounds.init();
		BrightestDayCreativeTabs.init();
		RingPowerRegistry.init();
		BrightestDayAttachments.init();
		RingPowerTicker.init();
		Networking.init();
		FlightRingPower.registerEvents();
		ShieldManager.init();
		TractorManager.init();
		ConstructTools.init();
		WallManager.init();
		SculptManager.init();
		RingJumpstart.init();
		BeamManager.init();
		HealBeamManager.init();
		CorpsSynergy.init();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
