package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record LanternRitualS2CPayload(int playerId, int mode, float yaw) implements CustomPacketPayload {
    public static final int NONE = 0;
    public static final int FLOOR = 1;
    public static final int TOP = 2;
    public static final int COMPLETE = 3;

    public static final Type<LanternRitualS2CPayload> TYPE =
            new Type<>(BrightestDay.id("lantern_ritual"));

    public static final StreamCodec<ByteBuf, LanternRitualS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, LanternRitualS2CPayload::playerId,
                    ByteBufCodecs.VAR_INT, LanternRitualS2CPayload::mode,
                    ByteBufCodecs.FLOAT, LanternRitualS2CPayload::yaw,
                    LanternRitualS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
