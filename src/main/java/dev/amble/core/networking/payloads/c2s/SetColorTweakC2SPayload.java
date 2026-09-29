package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.ringpowers.ColorTweak;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetColorTweakC2SPayload(ColorTweak tweak) implements CustomPacketPayload {

    public static final Type<SetColorTweakC2SPayload> TYPE =
            new Type<>(BrightestDay.id("set_color_tweak"));

    public static final StreamCodec<ByteBuf, SetColorTweakC2SPayload> CODEC =
            ColorTweak.STREAM_CODEC.map(SetColorTweakC2SPayload::new, SetColorTweakC2SPayload::tweak);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        BrightestDayAttachments.setColorTweak(context.player(), this.tweak);
    }
}
