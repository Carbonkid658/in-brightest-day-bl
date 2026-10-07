package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.impl.GatherRingPower;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record GatherC2SPayload() implements CustomPacketPayload {

    public static final GatherC2SPayload INSTANCE = new GatherC2SPayload();

    public static final Type<GatherC2SPayload> TYPE =
            new Type<>(BrightestDay.id("gather_tribe"));

    public static final StreamCodec<ByteBuf, GatherC2SPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        GatherRingPower.fire(context.player());
    }
}
