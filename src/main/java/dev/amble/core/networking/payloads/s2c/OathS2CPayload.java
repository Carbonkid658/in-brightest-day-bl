package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OathS2CPayload(String oath, int color, int reached, float progress, boolean guide, boolean active) implements CustomPacketPayload {
    public static final OathS2CPayload NONE = new OathS2CPayload("", 0, 0, 0.0F, false, false);

    public static final Type<OathS2CPayload> TYPE =
            new Type<>(BrightestDay.id("oath"));

    public static final StreamCodec<ByteBuf, OathS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, OathS2CPayload::oath,
                    ByteBufCodecs.INT, OathS2CPayload::color,
                    ByteBufCodecs.VAR_INT, OathS2CPayload::reached,
                    ByteBufCodecs.FLOAT, OathS2CPayload::progress,
                    ByteBufCodecs.BOOL, OathS2CPayload::guide,
                    ByteBufCodecs.BOOL, OathS2CPayload::active,
                    OathS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
