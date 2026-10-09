package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HealBeamS2CPayload(int playerId, int targetId, int color) implements CustomPacketPayload {

    public static final int NO_TARGET = -1;

    public static final Type<HealBeamS2CPayload> TYPE =
            new Type<>(BrightestDay.id("heal_beam"));

    public static final StreamCodec<ByteBuf, HealBeamS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, HealBeamS2CPayload::playerId,
                    ByteBufCodecs.INT, HealBeamS2CPayload::targetId,
                    ByteBufCodecs.INT, HealBeamS2CPayload::color,
                    HealBeamS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
