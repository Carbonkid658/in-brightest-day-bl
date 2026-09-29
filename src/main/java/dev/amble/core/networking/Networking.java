package dev.amble.core.networking;

import dev.amble.core.networking.payloads.c2s.CycleConstructC2SPayload;
import dev.amble.core.networking.payloads.c2s.FireConstructC2SPayload;
import dev.amble.core.networking.payloads.c2s.OpenLanternC2SPayload;
import dev.amble.core.networking.payloads.c2s.ScanC2SPayload;
import dev.amble.core.networking.payloads.c2s.SetColorTweakC2SPayload;
import dev.amble.core.networking.payloads.c2s.SetFlightC2SPayload;
import dev.amble.core.networking.payloads.c2s.ToggleLightC2SPayload;
import dev.amble.core.networking.payloads.c2s.TractorC2SPayload;
import dev.amble.core.networking.payloads.c2s.UsePowerC2SPayload;
import dev.amble.core.networking.payloads.s2c.BlastS2CPayload;
import dev.amble.core.networking.payloads.s2c.ScanS2CPayload;
import dev.amble.core.networking.payloads.s2c.ScanStartS2CPayload;
import dev.amble.core.networking.payloads.s2c.ShieldRemoveS2CPayload;
import dev.amble.core.networking.payloads.s2c.ShieldSpawnS2CPayload;
import dev.amble.core.networking.payloads.s2c.TractorS2CPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class Networking {
    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(UsePowerC2SPayload.TYPE, UsePowerC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SetFlightC2SPayload.TYPE, SetFlightC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OpenLanternC2SPayload.TYPE, OpenLanternC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FireConstructC2SPayload.TYPE, FireConstructC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ToggleLightC2SPayload.TYPE, ToggleLightC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CycleConstructC2SPayload.TYPE, CycleConstructC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SetColorTweakC2SPayload.TYPE, SetColorTweakC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TractorC2SPayload.TYPE, TractorC2SPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TractorS2CPayload.TYPE, TractorS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ScanS2CPayload.TYPE, ScanS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ScanStartS2CPayload.TYPE, ScanStartS2CPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ScanC2SPayload.TYPE, ScanC2SPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BlastS2CPayload.TYPE, BlastS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ShieldSpawnS2CPayload.TYPE, ShieldSpawnS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ShieldRemoveS2CPayload.TYPE, ShieldRemoveS2CPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(UsePowerC2SPayload.TYPE, UsePowerC2SPayload::handle);
        ServerPlayNetworking.registerGlobalReceiver(SetFlightC2SPayload.TYPE, SetFlightC2SPayload::handle);
        ServerPlayNetworking.registerGlobalReceiver(OpenLanternC2SPayload.TYPE, OpenLanternC2SPayload::handle);
        ServerPlayNetworking.registerGlobalReceiver(FireConstructC2SPayload.TYPE, FireConstructC2SPayload::handle);
        ServerPlayNetworking.registerGlobalReceiver(CycleConstructC2SPayload.TYPE, CycleConstructC2SPayload::handle);
        ServerPlayNetworking.registerGlobalReceiver(SetColorTweakC2SPayload.TYPE, SetColorTweakC2SPayload::handle);
        ServerPlayNetworking.registerGlobalReceiver(TractorC2SPayload.TYPE, TractorC2SPayload::handle);
        ServerPlayNetworking.registerGlobalReceiver(ScanC2SPayload.TYPE, ScanC2SPayload::handle);
        ServerPlayNetworking.registerGlobalReceiver(ToggleLightC2SPayload.TYPE, ToggleLightC2SPayload::handle);
    }
}
