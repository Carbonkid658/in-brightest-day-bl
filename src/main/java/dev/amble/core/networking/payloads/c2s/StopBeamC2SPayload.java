package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.beams.BeamManager;
import dev.amble.core.beams.HealBeamManager;
import dev.amble.core.sculpt.SculptManager;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record StopBeamC2SPayload() implements CustomPacketPayload {

    public static final StopBeamC2SPayload INSTANCE = new StopBeamC2SPayload();

    public static final Type<StopBeamC2SPayload> TYPE =
            new Type<>(BrightestDay.id("stop_beam"));

    public static final StreamCodec<ByteBuf, StopBeamC2SPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        BeamManager.stop(context.player());
        HealBeamManager.stop(context.player());
        SculptManager.stop(context.player(), true);
    }
}
