package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.impl.LightRingPower;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ToggleLightC2SPayload() implements CustomPacketPayload {

    public static final ToggleLightC2SPayload INSTANCE = new ToggleLightC2SPayload();

    public static final Type<ToggleLightC2SPayload> TYPE =
            new Type<>(BrightestDay.id("toggle_light"));

    public static final StreamCodec<ByteBuf, ToggleLightC2SPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        LightRingPower.toggle(context.player());
    }
}
