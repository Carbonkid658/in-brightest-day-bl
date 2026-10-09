package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ActiveConstructS2CPayload(int playerId, boolean active) implements CustomPacketPayload {

    public static final Type<ActiveConstructS2CPayload> TYPE =
            new Type<>(BrightestDay.id("active_construct"));

    public static final StreamCodec<ByteBuf, ActiveConstructS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ActiveConstructS2CPayload::playerId,
                    ByteBufCodecs.BOOL, ActiveConstructS2CPayload::active,
                    ActiveConstructS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
