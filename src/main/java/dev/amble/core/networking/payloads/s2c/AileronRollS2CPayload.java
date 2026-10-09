package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AileronRollS2CPayload(int playerId, boolean right) implements CustomPacketPayload {

    public static final Type<AileronRollS2CPayload> TYPE =
            new Type<>(BrightestDay.id("aileron_roll_effect"));

    public static final StreamCodec<ByteBuf, AileronRollS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, AileronRollS2CPayload::playerId,
                    ByteBufCodecs.BOOL, AileronRollS2CPayload::right,
                    AileronRollS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
