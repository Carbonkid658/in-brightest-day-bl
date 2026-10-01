package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record BarrageBoltS2CPayload(int playerId, Vec3 end, int color, boolean hit) implements CustomPacketPayload {

    public static final Type<BarrageBoltS2CPayload> TYPE =
            new Type<>(BrightestDay.id("rapid_barrage_bolt"));

    public static final StreamCodec<ByteBuf, BarrageBoltS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, BarrageBoltS2CPayload::playerId,
                    Vec3.STREAM_CODEC, BarrageBoltS2CPayload::end,
                    ByteBufCodecs.INT, BarrageBoltS2CPayload::color,
                    ByteBufCodecs.BOOL, BarrageBoltS2CPayload::hit,
                    BarrageBoltS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
