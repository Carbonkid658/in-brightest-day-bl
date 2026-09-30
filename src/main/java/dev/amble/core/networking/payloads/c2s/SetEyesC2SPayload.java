package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.ringpowers.EyePaint;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetEyesC2SPayload(EyePaint eyes) implements CustomPacketPayload {

    public static final Type<SetEyesC2SPayload> TYPE =
            new Type<>(BrightestDay.id("set_eyes"));

    public static final StreamCodec<ByteBuf, SetEyesC2SPayload> CODEC =
            EyePaint.STREAM_CODEC.map(SetEyesC2SPayload::new, SetEyesC2SPayload::eyes);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        BrightestDayAttachments.setEyes(context.player(), this.eyes);
    }
}
