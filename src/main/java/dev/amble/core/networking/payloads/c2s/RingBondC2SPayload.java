package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.loyalty.RingBonds;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RingBondC2SPayload(boolean relinquish) implements CustomPacketPayload {

    public static final Type<RingBondC2SPayload> TYPE =
            new Type<>(BrightestDay.id("ring_bond"));

    public static final StreamCodec<ByteBuf, RingBondC2SPayload> CODEC =
            ByteBufCodecs.BOOL.map(RingBondC2SPayload::new, RingBondC2SPayload::relinquish);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        if (this.relinquish) RingBonds.relinquish(context.player());
        else RingBonds.recall(context.player());
    }
}
