package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CompassS2CPayload(int playerId, BlockPos target, int color, int duration) implements CustomPacketPayload {

    public static final Type<CompassS2CPayload> TYPE =
            new Type<>(BrightestDay.id("ring_compass"));

    public static final StreamCodec<ByteBuf, CompassS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CompassS2CPayload::playerId,
                    BlockPos.STREAM_CODEC, CompassS2CPayload::target,
                    ByteBufCodecs.INT, CompassS2CPayload::color,
                    ByteBufCodecs.VAR_INT, CompassS2CPayload::duration,
                    CompassS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
