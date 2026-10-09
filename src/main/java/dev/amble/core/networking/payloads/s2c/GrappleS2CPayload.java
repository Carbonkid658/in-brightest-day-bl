package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record GrappleS2CPayload(int hookId, int ownerId, int color, Vec3 position, int state, int targetId) implements CustomPacketPayload {
    public static final int FLYING = 0;
    public static final int LATCHED = 1;
    public static final int RETRACTING = 2;
    public static final int GONE = 3;
    public static final int NO_TARGET = -1;

    public static final Type<GrappleS2CPayload> TYPE =
            new Type<>(BrightestDay.id("grappling_hook"));

    public static final StreamCodec<ByteBuf, GrappleS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, GrappleS2CPayload::hookId,
                    ByteBufCodecs.VAR_INT, GrappleS2CPayload::ownerId,
                    ByteBufCodecs.INT, GrappleS2CPayload::color,
                    Vec3.STREAM_CODEC, GrappleS2CPayload::position,
                    ByteBufCodecs.VAR_INT, GrappleS2CPayload::state,
                    ByteBufCodecs.VAR_INT, GrappleS2CPayload::targetId,
                    GrappleS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
