package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PoseS2CPayload(int playerId, int pose) implements CustomPacketPayload {

    public static final Type<PoseS2CPayload> TYPE =
            new Type<>(BrightestDay.id("pose_state"));

    public static final StreamCodec<ByteBuf, PoseS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PoseS2CPayload::playerId,
                    ByteBufCodecs.VAR_INT, PoseS2CPayload::pose,
                    PoseS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
