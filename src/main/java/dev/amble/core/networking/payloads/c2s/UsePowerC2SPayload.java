package dev.amble.core.networking.payloads.c2s;

import dev.amble.BrightestDay;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.RingPowerInstance;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public record UsePowerC2SPayload(int slot) implements CustomPacketPayload {

    public static final Type<UsePowerC2SPayload> TYPE =
            new Type<>(BrightestDay.id("use_power"));

    public static final StreamCodec<ByteBuf, UsePowerC2SPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, UsePowerC2SPayload::slot,
                    UsePowerC2SPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        List<RingPowerInstance<?>> powers = BrightestDayAttachments.slotted(player);

        if (slot < 0 || slot >= powers.size()) return;

        RingPowerInstance<?> instance = powers.get(slot);
        if (!instance.power().worksWithoutCharge() && !PowerRingItem.hasCharge(player)) {
            player.sendOverlayMessage(Component.translatable("message.brightestday.ring_depleted"));
            return;
        }
        instance.run(player);
    }
}