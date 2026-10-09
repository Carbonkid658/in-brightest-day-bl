package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DrillS2CPayload(int playerId, int color, int size, boolean active) implements CustomPacketPayload {

    public static final Type<DrillS2CPayload> TYPE =
            new Type<>(BrightestDay.id("drill"));

    public static final StreamCodec<ByteBuf, DrillS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, DrillS2CPayload::playerId,
                    ByteBufCodecs.INT, DrillS2CPayload::color,
                    ByteBufCodecs.VAR_INT, DrillS2CPayload::size,
                    ByteBufCodecs.BOOL, DrillS2CPayload::active,
                    DrillS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
