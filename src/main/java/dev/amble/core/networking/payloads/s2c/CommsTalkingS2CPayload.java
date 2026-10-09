package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CommsTalkingS2CPayload(int playerId, boolean talking) implements CustomPacketPayload {

    public static final Type<CommsTalkingS2CPayload> TYPE =
            new Type<>(BrightestDay.id("comms_talking"));

    public static final StreamCodec<ByteBuf, CommsTalkingS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CommsTalkingS2CPayload::playerId,
                    ByteBufCodecs.BOOL, CommsTalkingS2CPayload::talking,
                    CommsTalkingS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
