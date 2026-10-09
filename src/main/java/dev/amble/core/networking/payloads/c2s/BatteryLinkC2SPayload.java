package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BatteryLinkC2SPayload() implements CustomPacketPayload {

    public static final BatteryLinkC2SPayload INSTANCE = new BatteryLinkC2SPayload();

    public static final Type<BatteryLinkC2SPayload> TYPE =
            new Type<>(BrightestDay.id("battery_link"));

    public static final StreamCodec<ByteBuf, BatteryLinkC2SPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
//        BatteryLinkRingPower.fire(context.player());
    }
}
