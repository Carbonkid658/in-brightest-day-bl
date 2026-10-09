package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CommsIncomingS2CPayload(String name, boolean active) implements CustomPacketPayload {

    public static final Type<CommsIncomingS2CPayload> TYPE =
            new Type<>(BrightestDay.id("comms_incoming"));

    public static final StreamCodec<ByteBuf, CommsIncomingS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, CommsIncomingS2CPayload::name,
                    ByteBufCodecs.BOOL, CommsIncomingS2CPayload::active,
                    CommsIncomingS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
