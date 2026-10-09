package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record WhipS2CPayload(int playerId, Vec3 direction, int color, boolean lash, float length, float arc, float side, int ticks) implements CustomPacketPayload {

    public static final Type<WhipS2CPayload> TYPE =
            new Type<>(BrightestDay.id("energy_whip"));

    public static final StreamCodec<ByteBuf, WhipS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, WhipS2CPayload::playerId,
                    Vec3.STREAM_CODEC, WhipS2CPayload::direction,
                    ByteBufCodecs.INT, WhipS2CPayload::color,
                    ByteBufCodecs.BOOL, WhipS2CPayload::lash,
                    ByteBufCodecs.FLOAT, WhipS2CPayload::length,
                    ByteBufCodecs.FLOAT, WhipS2CPayload::arc,
                    ByteBufCodecs.FLOAT, WhipS2CPayload::side,
                    ByteBufCodecs.VAR_INT, WhipS2CPayload::ticks,
                    WhipS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
