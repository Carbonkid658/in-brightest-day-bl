package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record TurretS2CPayload(int id, int ownerId, Vec3 center, int color, int remaining, boolean present) implements CustomPacketPayload {

    public static final Type<TurretS2CPayload> TYPE =
            new Type<>(BrightestDay.id("sentry_turret"));

    public static final StreamCodec<ByteBuf, TurretS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TurretS2CPayload::id,
                    ByteBufCodecs.VAR_INT, TurretS2CPayload::ownerId,
                    Vec3.STREAM_CODEC, TurretS2CPayload::center,
                    ByteBufCodecs.INT, TurretS2CPayload::color,
                    ByteBufCodecs.VAR_INT, TurretS2CPayload::remaining,
                    ByteBufCodecs.BOOL, TurretS2CPayload::present,
                    TurretS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
