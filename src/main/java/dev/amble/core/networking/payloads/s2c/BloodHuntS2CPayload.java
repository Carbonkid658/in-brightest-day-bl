package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BloodHuntS2CPayload(int preyId, double x, double y, double z) implements CustomPacketPayload {
    public static final int NO_PREY = -1;
    public static final BloodHuntS2CPayload NONE = new BloodHuntS2CPayload(NO_PREY, 0.0, 0.0, 0.0);

    public static final Type<BloodHuntS2CPayload> TYPE =
            new Type<>(BrightestDay.id("blood_hunt"));

    public static final StreamCodec<ByteBuf, BloodHuntS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, BloodHuntS2CPayload::preyId,
                    ByteBufCodecs.DOUBLE, BloodHuntS2CPayload::x,
                    ByteBufCodecs.DOUBLE, BloodHuntS2CPayload::y,
                    ByteBufCodecs.DOUBLE, BloodHuntS2CPayload::z,
                    BloodHuntS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
