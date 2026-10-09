package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.impl.FlightRingPower;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FlightSpeedC2SPayload(int level) implements CustomPacketPayload {

    public static final Type<FlightSpeedC2SPayload> TYPE =
            new Type<>(BrightestDay.id("flight_speed"));

    public static final StreamCodec<ByteBuf, FlightSpeedC2SPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, FlightSpeedC2SPayload::level,
                    FlightSpeedC2SPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        FlightRingPower.setSpeedLevel(context.player(), this.level);
    }
}
