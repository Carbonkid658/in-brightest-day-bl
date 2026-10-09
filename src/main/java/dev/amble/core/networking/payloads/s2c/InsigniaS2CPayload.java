package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record InsigniaS2CPayload(int playerId, boolean active) implements CustomPacketPayload {

    public static final Type<InsigniaS2CPayload> TYPE =
            new Type<>(BrightestDay.id("insignia"));

    public static final StreamCodec<ByteBuf, InsigniaS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, InsigniaS2CPayload::playerId,
                    ByteBufCodecs.BOOL, InsigniaS2CPayload::active,
                    InsigniaS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
