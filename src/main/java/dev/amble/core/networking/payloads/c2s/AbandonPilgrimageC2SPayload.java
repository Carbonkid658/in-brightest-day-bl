package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.progression.Pilgrimage;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AbandonPilgrimageC2SPayload() implements CustomPacketPayload {

    public static final AbandonPilgrimageC2SPayload INSTANCE = new AbandonPilgrimageC2SPayload();

    public static final Type<AbandonPilgrimageC2SPayload> TYPE =
            new Type<>(BrightestDay.id("abandon_pilgrimage"));

    public static final StreamCodec<ByteBuf, AbandonPilgrimageC2SPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        Pilgrimage.abandon(context.player());
    }
}
