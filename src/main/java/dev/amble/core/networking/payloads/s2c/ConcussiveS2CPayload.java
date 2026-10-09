package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record ConcussiveS2CPayload(int playerId, Vec3 direction, int color) implements CustomPacketPayload {

    public static final Type<ConcussiveS2CPayload> TYPE =
            new Type<>(BrightestDay.id("concussive_blast"));

    public static final StreamCodec<ByteBuf, ConcussiveS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ConcussiveS2CPayload::playerId,
                    Vec3.STREAM_CODEC, ConcussiveS2CPayload::direction,
                    ByteBufCodecs.INT, ConcussiveS2CPayload::color,
                    ConcussiveS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
