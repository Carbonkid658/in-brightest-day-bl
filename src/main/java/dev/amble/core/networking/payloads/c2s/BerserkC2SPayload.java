package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.impl.BerserkRingPower;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BerserkC2SPayload() implements CustomPacketPayload {

    public static final BerserkC2SPayload INSTANCE = new BerserkC2SPayload();

    public static final Type<BerserkC2SPayload> TYPE =
            new Type<>(BrightestDay.id("berserk"));

    public static final StreamCodec<ByteBuf, BerserkC2SPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        BerserkRingPower.fire(context.player());
    }
}
