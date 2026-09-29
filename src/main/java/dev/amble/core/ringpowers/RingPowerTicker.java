package dev.amble.core.ringpowers;

import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.constructs.ConstructTools;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

public final class RingPowerTicker {
    private static final Map<ServerPlayer, Boolean> CHARGED = new WeakHashMap<>();


    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(RingPowerTicker::tick);
    }

    private static void tick(MinecraftServer server) {
        boolean drainTick = server.getTickCount() % 20 == 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ItemStack ring = BrightestDayAttachments.getRing(player);
            if (!ring.isEmpty() && PowerRingItem.tickCharge(ring, player.level())) {
                BrightestDayAttachments.setRing(player, ring);
            }

            BrightestDayAttachments.sync(player, PowerRingItem.getWornCorps(player).orElse(null));
            RingBenefits.tick(player, server.getTickCount());
            ConstructTools.tick(player, server.getTickCount());
            depleteOnEmpty(player);

            for (RingPowerInstance<?> instance : BrightestDayAttachments.get(player)) {
                instance.tick(player);
                if (drainTick) drain(player, instance);
            }
        }
    }

    private static void depleteOnEmpty(ServerPlayer player) {
        boolean charged = PowerRingItem.hasCharge(player);
        Boolean wasCharged = CHARGED.put(player, charged);
        if (charged || Boolean.FALSE.equals(wasCharged)) return;

        for (RingPowerInstance<?> instance : BrightestDayAttachments.get(player)) {
            if (!instance.power().worksWithoutCharge()) instance.onDepleted(player);
        }
    }

    private static void drain(ServerPlayer player, RingPowerInstance<?> instance) {
        if (player.hasInfiniteMaterials()) return;

        int drain = instance.drainPerSecond(player);
        if (drain > 0 && !PowerRingItem.drainWorn(player, drain)) {
            instance.onDepleted(player);
        }
    }

    private RingPowerTicker() {}
}
