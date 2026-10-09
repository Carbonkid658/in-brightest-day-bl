package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.impl.ConversionRingPower;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ConversionC2SPayload() implements CustomPacketPayload {

    public static final ConversionC2SPayload INSTANCE = new ConversionC2SPayload();

    public static final Type<ConversionC2SPayload> TYPE =
            new Type<>(BrightestDay.id("indigo_conversion"));

    public static final StreamCodec<ByteBuf, ConversionC2SPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        ConversionRingPower.fire(context.player());
    }
}
