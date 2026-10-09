package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.impl.FlightRingPower;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetFlightC2SPayload(boolean enabled) implements CustomPacketPayload {

    public static final Type<SetFlightC2SPayload> TYPE =
            new Type<>(BrightestDay.id("set_flight"));

    public static final StreamCodec<ByteBuf, SetFlightC2SPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, SetFlightC2SPayload::enabled,
                    SetFlightC2SPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        FlightRingPower.setEnabled(context.player(), this.enabled);
    }
}
