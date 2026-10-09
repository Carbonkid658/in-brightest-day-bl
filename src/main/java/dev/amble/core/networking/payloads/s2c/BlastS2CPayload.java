package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record BlastS2CPayload(int shooterId, Vec3 impact, int color, float scale) implements CustomPacketPayload {

    public static final Type<BlastS2CPayload> TYPE =
            new Type<>(BrightestDay.id("blast"));

    public static final StreamCodec<ByteBuf, BlastS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, BlastS2CPayload::shooterId,
                    Vec3.STREAM_CODEC, BlastS2CPayload::impact,
                    ByteBufCodecs.INT, BlastS2CPayload::color,
                    ByteBufCodecs.FLOAT, BlastS2CPayload::scale,
                    BlastS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
