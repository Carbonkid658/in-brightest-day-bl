package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record GlideS2CPayload(int entityId, int casterId, int color, int remaining, boolean present) implements CustomPacketPayload {

    public static final Type<GlideS2CPayload> TYPE =
            new Type<>(BrightestDay.id("glide"));

    public static final StreamCodec<ByteBuf, GlideS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, GlideS2CPayload::entityId,
                    ByteBufCodecs.VAR_INT, GlideS2CPayload::casterId,
                    ByteBufCodecs.INT, GlideS2CPayload::color,
                    ByteBufCodecs.VAR_INT, GlideS2CPayload::remaining,
                    ByteBufCodecs.BOOL, GlideS2CPayload::present,
                    GlideS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
