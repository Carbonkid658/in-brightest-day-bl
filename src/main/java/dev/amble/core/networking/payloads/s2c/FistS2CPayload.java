package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record FistS2CPayload(Vec3 origin, Vec3 direction, float reach, int color, int windup, int travel, boolean impact) implements CustomPacketPayload {

    public static final Type<FistS2CPayload> TYPE =
            new Type<>(BrightestDay.id("giant_fist"));

    public static final StreamCodec<ByteBuf, FistS2CPayload> CODEC =
            StreamCodec.composite(
                    Vec3.STREAM_CODEC, FistS2CPayload::origin,
                    Vec3.STREAM_CODEC, FistS2CPayload::direction,
                    ByteBufCodecs.FLOAT, FistS2CPayload::reach,
                    ByteBufCodecs.INT, FistS2CPayload::color,
                    ByteBufCodecs.VAR_INT, FistS2CPayload::windup,
                    ByteBufCodecs.VAR_INT, FistS2CPayload::travel,
                    ByteBufCodecs.BOOL, FistS2CPayload::impact,
                    FistS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
