package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MegaphoneS2CPayload(int playerId, boolean active) implements CustomPacketPayload {

    public static final Type<MegaphoneS2CPayload> TYPE =
            new Type<>(BrightestDay.id("megaphone"));

    public static final StreamCodec<ByteBuf, MegaphoneS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, MegaphoneS2CPayload::playerId,
                    ByteBufCodecs.BOOL, MegaphoneS2CPayload::active,
                    MegaphoneS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
