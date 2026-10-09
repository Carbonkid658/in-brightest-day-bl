package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TractorS2CPayload(int playerId, int targetId) implements CustomPacketPayload {

    public static final int NO_TARGET = -1;

    public static final Type<TractorS2CPayload> TYPE =
            new Type<>(BrightestDay.id("tractor"));

    public static final StreamCodec<ByteBuf, TractorS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TractorS2CPayload::playerId,
                    ByteBufCodecs.INT, TractorS2CPayload::targetId,
                    TractorS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
