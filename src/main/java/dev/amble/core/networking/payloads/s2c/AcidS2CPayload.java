package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AcidS2CPayload(int playerId, boolean active) implements CustomPacketPayload {

    public static final Type<AcidS2CPayload> TYPE =
            new Type<>(BrightestDay.id("acid_vomit"));

    public static final StreamCodec<ByteBuf, AcidS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, AcidS2CPayload::playerId,
                    ByteBufCodecs.BOOL, AcidS2CPayload::active,
                    AcidS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
