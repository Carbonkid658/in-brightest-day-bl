package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record GlideBoltS2CPayload(int casterId, int targetId, int color) implements CustomPacketPayload {

    public static final Type<GlideBoltS2CPayload> TYPE =
            new Type<>(BrightestDay.id("glide_bolt"));

    public static final StreamCodec<ByteBuf, GlideBoltS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, GlideBoltS2CPayload::casterId,
                    ByteBufCodecs.VAR_INT, GlideBoltS2CPayload::targetId,
                    ByteBufCodecs.INT, GlideBoltS2CPayload::color,
                    GlideBoltS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
