package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record DiscS2CPayload(int discId, int color, Vec3 position, Vec3 direction, boolean alive) implements CustomPacketPayload {

    public static final Type<DiscS2CPayload> TYPE =
            new Type<>(BrightestDay.id("boomerang_disc"));

    public static final StreamCodec<ByteBuf, DiscS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, DiscS2CPayload::discId,
                    ByteBufCodecs.INT, DiscS2CPayload::color,
                    Vec3.STREAM_CODEC, DiscS2CPayload::position,
                    Vec3.STREAM_CODEC, DiscS2CPayload::direction,
                    ByteBufCodecs.BOOL, DiscS2CPayload::alive,
                    DiscS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
