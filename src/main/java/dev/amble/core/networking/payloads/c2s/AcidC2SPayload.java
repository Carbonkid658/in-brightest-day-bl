package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.acid.AcidManager;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AcidC2SPayload(boolean active) implements CustomPacketPayload {

    public static final Type<AcidC2SPayload> TYPE =
            new Type<>(BrightestDay.id("acid_vomit"));

    public static final StreamCodec<ByteBuf, AcidC2SPayload> CODEC =
            ByteBufCodecs.BOOL.map(AcidC2SPayload::new, AcidC2SPayload::active);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        if (this.active) {
            AcidManager.start(context.player());
        } else {
            AcidManager.stop(context.player());
        }
    }
}
