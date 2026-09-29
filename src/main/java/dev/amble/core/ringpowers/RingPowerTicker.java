package dev.amble.core.ringpowers;

import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.BrightestDaySounds;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.constructs.ConstructDismissal;
import dev.amble.core.ringpowers.constructs.ConstructTools;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

public final class RingPowerTicker {
    private static final Map<ServerPlayer, Boolean> CHARGED = new WeakHashMap<>();
    private static final int DEPLETION_RECHECK_TICKS = 10;
    private static final float LOW_CHARGE = 0.05F;
    private static final Map<ServerPlayer, Float> LAST_CHARGE = new WeakHashMap<>();


    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(RingPowerTicker::tick);
    }

    private static void tick(MinecraftServer server) {
        boolean drainTick = server.getTickCount() % 20 == 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ItemStack ring = BrightestDayAttachments.getRing(player);
            if (!ring.isEmpty() && !CorpsSynergy.empoweredByHope(player) && PowerRingItem.tickCharge(ring, player.level())) {
                BrightestDayAttachments.setRing(player, ring);
            }

            BrightestDayAttachments.sync(player, PowerRingItem.getWornCorps(player).orElse(null));
            RingBenefits.tick(player, server.getTickCount());
            ConstructTools.tick(player, server.getTickCount());
            depleteOnEmpty(player, server.getTickCount());
            warnOnLowCharge(player);

            for (RingPowerInstance<?> instance : BrightestDayAttachments.get(player)) {
                instance.tick(player);
                if (drainTick) drain(player, instance);
            }
        }
    }

    private static void warnOnLowCharge(ServerPlayer player) {
        ItemStack ring = PowerRingItem.getWornRing(player);
        if (ring.isEmpty()) {
            LAST_CHARGE.remove(player);
            return;
        }

        float charge = PowerRingItem.getChargeFraction(ring);
        Float previous = LAST_CHARGE.put(player, charge);
        if (previous != null && previous > LOW_CHARGE && charge <= LOW_CHARGE && charge > 0.0F) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    BrightestDaySounds.RING_CHARGE_5_PERCENT, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static void depleteOnEmpty(ServerPlayer player, int serverTick) {
        boolean charged = PowerRingItem.hasCharge(player);
        Boolean wasCharged = CHARGED.put(player, charged);
        if (charged) return;

        boolean justDied = !Boolean.FALSE.equals(wasCharged);
        if (justDied) {
            ArmedRingPower.lower(player);
            ConstructDismissal.dismissAll(player);
        }
        if (!justDied && serverTick % DEPLETION_RECHECK_TICKS != 0) return;

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
