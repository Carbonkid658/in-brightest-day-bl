package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ScanStartS2CPayload(int entityId, BlockPos pos, int color) implements CustomPacketPayload {

    public static final Type<ScanStartS2CPayload> TYPE =
            new Type<>(BrightestDay.id("scan_start"));

    public static final StreamCodec<ByteBuf, ScanStartS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, ScanStartS2CPayload::entityId,
                    BlockPos.STREAM_CODEC, ScanStartS2CPayload::pos,
                    ByteBufCodecs.INT, ScanStartS2CPayload::color,
                    ScanStartS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
