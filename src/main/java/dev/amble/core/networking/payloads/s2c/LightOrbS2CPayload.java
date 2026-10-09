package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record LightOrbS2CPayload(int id, int casterId, Vec3 center, int size, int color, boolean present) implements CustomPacketPayload {

    public static final Type<LightOrbS2CPayload> TYPE =
            new Type<>(BrightestDay.id("light_orb"));

    public static final StreamCodec<ByteBuf, LightOrbS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, LightOrbS2CPayload::id,
                    ByteBufCodecs.VAR_INT, LightOrbS2CPayload::casterId,
                    Vec3.STREAM_CODEC, LightOrbS2CPayload::center,
                    ByteBufCodecs.VAR_INT, LightOrbS2CPayload::size,
                    ByteBufCodecs.INT, LightOrbS2CPayload::color,
                    ByteBufCodecs.BOOL, LightOrbS2CPayload::present,
                    LightOrbS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
