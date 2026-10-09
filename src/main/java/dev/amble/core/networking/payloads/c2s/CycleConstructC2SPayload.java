package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CycleConstructC2SPayload() implements CustomPacketPayload {

    public static final CycleConstructC2SPayload INSTANCE = new CycleConstructC2SPayload();

    public static final Type<CycleConstructC2SPayload> TYPE =
            new Type<>(BrightestDay.id("cycle_construct"));

    public static final StreamCodec<ByteBuf, CycleConstructC2SPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        ArmedRingPower.cycle(context.player());
    }
}
