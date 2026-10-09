package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.impl.ConcussiveRingPower;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ConcussiveC2SPayload() implements CustomPacketPayload {

    public static final ConcussiveC2SPayload INSTANCE = new ConcussiveC2SPayload();

    public static final Type<ConcussiveC2SPayload> TYPE =
            new Type<>(BrightestDay.id("concussive_blast"));

    public static final StreamCodec<ByteBuf, ConcussiveC2SPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        ConcussiveRingPower.fire(context.player());
    }
}
