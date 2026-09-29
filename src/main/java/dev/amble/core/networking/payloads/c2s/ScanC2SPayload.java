package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.impl.ScanRingPower;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ScanC2SPayload(int action) implements CustomPacketPayload {

    public static final int START = 0;
    public static final int CANCEL = 1;
    public static final int COMPLETE = 2;

    public static final Type<ScanC2SPayload> TYPE =
            new Type<>(BrightestDay.id("scan_control"));

    public static final StreamCodec<ByteBuf, ScanC2SPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ScanC2SPayload::action,
                    ScanC2SPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        switch (this.action) {
            case START -> ScanRingPower.start(context.player());
            case CANCEL -> ScanRingPower.cancel(context.player());
            case COMPLETE -> ScanRingPower.complete(context.player());
            default -> {}
        }
    }
}
