package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.sculpt.SculptManager;
import dev.amble.core.sculpt.SculptShape;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SculptShapeC2SPayload(int shape) implements CustomPacketPayload {

    public static final Type<SculptShapeC2SPayload> TYPE =
            new Type<>(BrightestDay.id("sculpt_shape"));

    public static final StreamCodec<ByteBuf, SculptShapeC2SPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SculptShapeC2SPayload::shape,
                    SculptShapeC2SPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        SculptManager.setShape(context.player(), SculptShape.byId(this.shape));
    }
}
