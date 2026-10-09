package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FireConstructC2SPayload(int radius) implements CustomPacketPayload {

    public static final Type<FireConstructC2SPayload> TYPE =
            new Type<>(BrightestDay.id("fire_construct"));

    public static final StreamCodec<ByteBuf, FireConstructC2SPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, FireConstructC2SPayload::radius,
                    FireConstructC2SPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        ArmedRingPower.fire(context.player(), this.radius);
    }
}
