package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record NovaS2CPayload(Vec3 center, float radius, int color) implements CustomPacketPayload {

    public static final Type<NovaS2CPayload> TYPE =
            new Type<>(BrightestDay.id("nova_burst"));

    public static final StreamCodec<ByteBuf, NovaS2CPayload> CODEC =
            StreamCodec.composite(
                    Vec3.STREAM_CODEC, NovaS2CPayload::center,
                    ByteBufCodecs.FLOAT, NovaS2CPayload::radius,
                    ByteBufCodecs.INT, NovaS2CPayload::color,
                    NovaS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
