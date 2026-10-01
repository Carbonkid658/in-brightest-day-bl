package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public record SwarmS2CPayload(int volleyId, int color, List<Vec3> positions, int alive, int burst) implements CustomPacketPayload {

    public static final Type<SwarmS2CPayload> TYPE =
            new Type<>(BrightestDay.id("swarm_missiles"));

    public static final StreamCodec<ByteBuf, SwarmS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SwarmS2CPayload::volleyId,
                    ByteBufCodecs.INT, SwarmS2CPayload::color,
                    Vec3.STREAM_CODEC.apply(ByteBufCodecs.list(16)), SwarmS2CPayload::positions,
                    ByteBufCodecs.VAR_INT, SwarmS2CPayload::alive,
                    ByteBufCodecs.VAR_INT, SwarmS2CPayload::burst,
                    SwarmS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
