package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BarrageS2CPayload(int playerId, int color, boolean active) implements CustomPacketPayload {

    public static final Type<BarrageS2CPayload> TYPE =
            new Type<>(BrightestDay.id("rapid_barrage"));

    public static final StreamCodec<ByteBuf, BarrageS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, BarrageS2CPayload::playerId,
                    ByteBufCodecs.INT, BarrageS2CPayload::color,
                    ByteBufCodecs.BOOL, BarrageS2CPayload::active,
                    BarrageS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
