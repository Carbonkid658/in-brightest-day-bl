package dev.amble.core.ringpowers;

import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.items.PowerRingItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class RingPowerTicker {

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(RingPowerTicker::tick);
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BrightestDayAttachments.sync(player, PowerRingItem.getWornCorps(player).orElse(null));

            for (RingPowerInstance<?> instance : BrightestDayAttachments.get(player)) {
                instance.tick(player);
            }
        }
    }

    private RingPowerTicker() {}
}
