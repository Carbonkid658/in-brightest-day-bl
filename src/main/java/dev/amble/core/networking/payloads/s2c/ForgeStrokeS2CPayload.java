package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import dev.amble.core.networking.payloads.c2s.ForgeStrokeC2SPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public record ForgeStrokeS2CPayload(int playerId, int action, List<Vec3> directions) implements CustomPacketPayload {

    public static final Type<ForgeStrokeS2CPayload> TYPE =
            new Type<>(BrightestDay.id("forge_stroke_relay"));

    public static final StreamCodec<ByteBuf, ForgeStrokeS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ForgeStrokeS2CPayload::playerId,
                    ByteBufCodecs.VAR_INT, ForgeStrokeS2CPayload::action,
                    Vec3.STREAM_CODEC.apply(ByteBufCodecs.list(ForgeStrokeC2SPayload.MAX_POINTS)), ForgeStrokeS2CPayload::directions,
                    ForgeStrokeS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
