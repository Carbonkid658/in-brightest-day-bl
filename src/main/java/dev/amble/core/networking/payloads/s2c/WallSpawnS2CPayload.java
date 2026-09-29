package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record WallSpawnS2CPayload(int id, List<BlockPos> cells, int color, int duration, int age) implements CustomPacketPayload {

    public static final Type<WallSpawnS2CPayload> TYPE =
            new Type<>(BrightestDay.id("wall_spawn"));

    public static final StreamCodec<ByteBuf, WallSpawnS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, WallSpawnS2CPayload::id,
                    BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), WallSpawnS2CPayload::cells,
                    ByteBufCodecs.INT, WallSpawnS2CPayload::color,
                    ByteBufCodecs.VAR_INT, WallSpawnS2CPayload::duration,
                    ByteBufCodecs.VAR_INT, WallSpawnS2CPayload::age,
                    WallSpawnS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
