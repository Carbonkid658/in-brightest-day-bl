package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OreProbeS2CPayload(int id, int ownerId, int color, int remaining, boolean present) implements CustomPacketPayload {

    public static final Type<OreProbeS2CPayload> TYPE =
            new Type<>(BrightestDay.id("ore_probe"));

    public static final StreamCodec<ByteBuf, OreProbeS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, OreProbeS2CPayload::id,
                    ByteBufCodecs.VAR_INT, OreProbeS2CPayload::ownerId,
                    ByteBufCodecs.INT, OreProbeS2CPayload::color,
                    ByteBufCodecs.VAR_INT, OreProbeS2CPayload::remaining,
                    ByteBufCodecs.BOOL, OreProbeS2CPayload::present,
                    OreProbeS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
