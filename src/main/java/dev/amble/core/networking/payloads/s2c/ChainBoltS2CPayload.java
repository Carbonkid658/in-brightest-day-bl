package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public record ChainBoltS2CPayload(int shooterId, List<Vec3> points, boolean struck, int color) implements CustomPacketPayload {

    public static final Type<ChainBoltS2CPayload> TYPE =
            new Type<>(BrightestDay.id("chain_bolt"));

    public static final StreamCodec<ByteBuf, ChainBoltS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ChainBoltS2CPayload::shooterId,
                    Vec3.STREAM_CODEC.apply(ByteBufCodecs.list(16)), ChainBoltS2CPayload::points,
                    ByteBufCodecs.BOOL, ChainBoltS2CPayload::struck,
                    ByteBufCodecs.INT, ChainBoltS2CPayload::color,
                    ChainBoltS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
