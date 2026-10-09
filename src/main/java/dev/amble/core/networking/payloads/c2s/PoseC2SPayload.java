package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.poses.Poses;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PoseC2SPayload(int pose) implements CustomPacketPayload {

    public static final Type<PoseC2SPayload> TYPE =
            new Type<>(BrightestDay.id("pose"));

    public static final StreamCodec<ByteBuf, PoseC2SPayload> CODEC =
            ByteBufCodecs.VAR_INT.map(PoseC2SPayload::new, PoseC2SPayload::pose);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        Poses.set(context.player(), this.pose);
    }
}
