package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.tractor.TractorManager;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TractorC2SPayload(int action, int steps) implements CustomPacketPayload {

    public static final int GRAB = 0;
    public static final int RELEASE = 1;
    public static final int ADJUST = 2;

    public static final Type<TractorC2SPayload> TYPE =
            new Type<>(BrightestDay.id("tractor_control"));

    public static final StreamCodec<ByteBuf, TractorC2SPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TractorC2SPayload::action,
                    ByteBufCodecs.INT, TractorC2SPayload::steps,
                    TractorC2SPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        switch (this.action) {
            case GRAB -> TractorManager.grab(context.player());
            case RELEASE -> TractorManager.release(context.player());
            case ADJUST -> TractorManager.adjust(context.player(), this.steps);
            default -> {}
        }
    }
}
