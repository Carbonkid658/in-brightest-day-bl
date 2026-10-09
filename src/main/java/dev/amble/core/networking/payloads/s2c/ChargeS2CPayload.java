package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ChargeS2CPayload(int playerId, boolean charging, int ticks) implements CustomPacketPayload {

    public static final Type<ChargeS2CPayload> TYPE =
            new Type<>(BrightestDay.id("charge"));

    public static final StreamCodec<ByteBuf, ChargeS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ChargeS2CPayload::playerId,
                    ByteBufCodecs.BOOL, ChargeS2CPayload::charging,
                    ByteBufCodecs.VAR_INT, ChargeS2CPayload::ticks,
                    ChargeS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
