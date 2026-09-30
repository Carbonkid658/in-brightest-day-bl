package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SelectConstructC2SPayload(Identifier construct) implements CustomPacketPayload {

    public static final Type<SelectConstructC2SPayload> TYPE =
            new Type<>(BrightestDay.id("select_construct"));

    public static final StreamCodec<ByteBuf, SelectConstructC2SPayload> CODEC =
            Identifier.STREAM_CODEC.map(SelectConstructC2SPayload::new, SelectConstructC2SPayload::construct);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        ArmedRingPower.select(context.player(), this.construct);
    }
}
