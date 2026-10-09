package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ShieldRemoveS2CPayload(int id) implements CustomPacketPayload {

    public static final Type<ShieldRemoveS2CPayload> TYPE =
            new Type<>(BrightestDay.id("shield_remove"));

    public static final StreamCodec<ByteBuf, ShieldRemoveS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ShieldRemoveS2CPayload::id,
                    ShieldRemoveS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
