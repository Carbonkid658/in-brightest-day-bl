package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TintFlashS2CPayload(int color, int ticks) implements CustomPacketPayload {

    public static final Type<TintFlashS2CPayload> TYPE =
            new Type<>(BrightestDay.id("tint_flash"));

    public static final StreamCodec<ByteBuf, TintFlashS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, TintFlashS2CPayload::color,
                    ByteBufCodecs.VAR_INT, TintFlashS2CPayload::ticks,
                    TintFlashS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
