package dev.amble.core.networking.payloads.s2c;

import dev.amble.BrightestDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record WallRemoveS2CPayload(int id) implements CustomPacketPayload {

    public static final Type<WallRemoveS2CPayload> TYPE =
            new Type<>(BrightestDay.id("wall_remove"));

    public static final StreamCodec<ByteBuf, WallRemoveS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, WallRemoveS2CPayload::id,
                    WallRemoveS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
