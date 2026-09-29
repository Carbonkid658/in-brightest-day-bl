package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.constructs.ConstructDismissal;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DismissConstructC2SPayload() implements CustomPacketPayload {

    public static final DismissConstructC2SPayload INSTANCE = new DismissConstructC2SPayload();

    public static final Type<DismissConstructC2SPayload> TYPE =
            new Type<>(BrightestDay.id("dismiss_construct"));

    public static final StreamCodec<ByteBuf, DismissConstructC2SPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        ConstructDismissal.dismissLatest(context.player());
    }
}
