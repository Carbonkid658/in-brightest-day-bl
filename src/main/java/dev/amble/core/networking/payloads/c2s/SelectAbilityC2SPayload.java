package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SelectAbilityC2SPayload(Identifier ability) implements CustomPacketPayload {

    public static final Type<SelectAbilityC2SPayload> TYPE =
            new Type<>(BrightestDay.id("select_ability"));

    public static final StreamCodec<ByteBuf, SelectAbilityC2SPayload> CODEC =
            Identifier.STREAM_CODEC.map(SelectAbilityC2SPayload::new, SelectAbilityC2SPayload::ability);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        ArmedRingPower.selectAbility(context.player(), this.ability);
    }
}
