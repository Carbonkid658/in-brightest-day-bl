package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BeamS2CPayload(int playerId, int color, boolean active) implements CustomPacketPayload {

    public static final Type<BeamS2CPayload> TYPE =
            new Type<>(BrightestDay.id("beam"));

    public static final StreamCodec<ByteBuf, BeamS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, BeamS2CPayload::playerId,
                    ByteBufCodecs.INT, BeamS2CPayload::color,
                    ByteBufCodecs.BOOL, BeamS2CPayload::active,
                    BeamS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
