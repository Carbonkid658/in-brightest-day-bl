package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record TurretBoltS2CPayload(int turretId, Vec3 from, int targetId, int color, float speed) implements CustomPacketPayload {

    public static final Type<TurretBoltS2CPayload> TYPE =
            new Type<>(BrightestDay.id("sentry_turret_bolt"));

    public static final StreamCodec<ByteBuf, TurretBoltS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TurretBoltS2CPayload::turretId,
                    Vec3.STREAM_CODEC, TurretBoltS2CPayload::from,
                    ByteBufCodecs.VAR_INT, TurretBoltS2CPayload::targetId,
                    ByteBufCodecs.INT, TurretBoltS2CPayload::color,
                    ByteBufCodecs.FLOAT, TurretBoltS2CPayload::speed,
                    TurretBoltS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
