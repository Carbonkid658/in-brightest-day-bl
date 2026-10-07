package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SphereS2CPayload(int playerId, float radius, int color) implements CustomPacketPayload {

    public static final Type<SphereS2CPayload> TYPE =
            new Type<>(BrightestDay.id("containment_sphere"));

    public static final StreamCodec<ByteBuf, SphereS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SphereS2CPayload::playerId,
                    ByteBufCodecs.FLOAT, SphereS2CPayload::radius,
                    ByteBufCodecs.INT, SphereS2CPayload::color,
                    SphereS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
