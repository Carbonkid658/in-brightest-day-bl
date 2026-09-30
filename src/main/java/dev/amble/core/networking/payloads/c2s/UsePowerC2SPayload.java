package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.RingPowerInstance;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record UsePowerC2SPayload(Identifier power) implements CustomPacketPayload {

    public static final Type<UsePowerC2SPayload> TYPE =
            new Type<>(BrightestDay.id("use_power"));

    public static final StreamCodec<ByteBuf, UsePowerC2SPayload> CODEC =
            Identifier.STREAM_CODEC.map(UsePowerC2SPayload::new, UsePowerC2SPayload::power);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        for (RingPowerInstance<?> instance : BrightestDayAttachments.get(player)) {
            if (!instance.power().id().equals(this.power)) continue;
            if (!instance.power().worksWithoutCharge() && !PowerRingItem.hasCharge(player)) {
                player.sendOverlayMessage(Component.translatable("message.brightestday.ring_depleted"));
                return;
            }
            instance.run(player);
            return;
        }
    }
}
