package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;

public record SanctuaryS2CPayload(Optional<BlockPos> center) implements CustomPacketPayload {

    public static final Type<SanctuaryS2CPayload> TYPE =
            new Type<>(BrightestDay.id("sanctuary"));

    public static final StreamCodec<ByteBuf, SanctuaryS2CPayload> CODEC =
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC).map(SanctuaryS2CPayload::new, SanctuaryS2CPayload::center);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
