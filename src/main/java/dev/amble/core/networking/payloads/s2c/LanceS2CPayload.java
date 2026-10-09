package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public record LanceS2CPayload(int shooterId, Vec3 end, List<Vec3> hits, int color) implements CustomPacketPayload {

    public static final Type<LanceS2CPayload> TYPE =
            new Type<>(BrightestDay.id("piercing_lance"));

    public static final StreamCodec<ByteBuf, LanceS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, LanceS2CPayload::shooterId,
                    Vec3.STREAM_CODEC, LanceS2CPayload::end,
                    Vec3.STREAM_CODEC.apply(ByteBufCodecs.list(64)), LanceS2CPayload::hits,
                    ByteBufCodecs.INT, LanceS2CPayload::color,
                    LanceS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
