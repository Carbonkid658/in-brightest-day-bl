package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CommsTargetS2CPayload(String name) implements CustomPacketPayload {
    public static final CommsTargetS2CPayload NONE = new CommsTargetS2CPayload("");

    public static final Type<CommsTargetS2CPayload> TYPE =
            new Type<>(BrightestDay.id("comms_target"));

    public static final StreamCodec<ByteBuf, CommsTargetS2CPayload> CODEC =
            ByteBufCodecs.STRING_UTF8.map(CommsTargetS2CPayload::new, CommsTargetS2CPayload::name);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
