package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record LumberjackS2CPayload(int id, int ownerId, Vec3 anchor, float yaw, int color, int remaining, boolean present) implements CustomPacketPayload {

    public static final Type<LumberjackS2CPayload> TYPE =
            new Type<>(BrightestDay.id("lumberjack"));

    public static final StreamCodec<ByteBuf, LumberjackS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, LumberjackS2CPayload::id,
                    ByteBufCodecs.VAR_INT, LumberjackS2CPayload::ownerId,
                    Vec3.STREAM_CODEC, LumberjackS2CPayload::anchor,
                    ByteBufCodecs.FLOAT, LumberjackS2CPayload::yaw,
                    ByteBufCodecs.INT, LumberjackS2CPayload::color,
                    ByteBufCodecs.VAR_INT, LumberjackS2CPayload::remaining,
                    ByteBufCodecs.BOOL, LumberjackS2CPayload::present,
                    LumberjackS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
