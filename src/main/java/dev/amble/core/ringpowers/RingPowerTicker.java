package dev.amble.core.ringpowers;

import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.items.PowerRingItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class RingPowerTicker {

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

            for (RingPowerInstance<?> instance : BrightestDayAttachments.get(player)) {
                instance.tick(player);
                if (drainTick) drain(player, instance);
            }
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
