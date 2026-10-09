package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PlacedDrillS2CPayload(int id, int ownerId, BlockPos head, Direction direction, int color, int size, boolean present) implements CustomPacketPayload {

    public static final Type<PlacedDrillS2CPayload> TYPE =
            new Type<>(BrightestDay.id("placed_drill"));

    public static final StreamCodec<ByteBuf, PlacedDrillS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PlacedDrillS2CPayload::id,
                    ByteBufCodecs.VAR_INT, PlacedDrillS2CPayload::ownerId,
                    BlockPos.STREAM_CODEC, PlacedDrillS2CPayload::head,
                    Direction.STREAM_CODEC, PlacedDrillS2CPayload::direction,
                    ByteBufCodecs.INT, PlacedDrillS2CPayload::color,
                    ByteBufCodecs.VAR_INT, PlacedDrillS2CPayload::size,
                    ByteBufCodecs.BOOL, PlacedDrillS2CPayload::present,
                    PlacedDrillS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
