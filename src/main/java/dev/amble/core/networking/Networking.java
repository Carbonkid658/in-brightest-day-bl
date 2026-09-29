package dev.amble.core.networking;

import dev.amble.core.networking.payloads.c2s.OpenLanternC2SPayload;
import dev.amble.core.networking.payloads.c2s.SetFlightC2SPayload;
import dev.amble.core.networking.payloads.c2s.UsePowerC2SPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class Networking {
    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(UsePowerC2SPayload.TYPE, UsePowerC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SetFlightC2SPayload.TYPE, SetFlightC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OpenLanternC2SPayload.TYPE, OpenLanternC2SPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(UsePowerC2SPayload.TYPE, UsePowerC2SPayload::handle);
        ServerPlayNetworking.registerGlobalReceiver(SetFlightC2SPayload.TYPE, SetFlightC2SPayload::handle);
        ServerPlayNetworking.registerGlobalReceiver(OpenLanternC2SPayload.TYPE, OpenLanternC2SPayload::handle);
    }
}
