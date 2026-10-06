package dev.amble.core.comms;

import dev.amble.core.items.PowerRingItem;
import dev.amble.core.networking.payloads.s2c.MegaphoneS2CPayload;
import dev.amble.core.ringpowers.RingPowerRegistry;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Megaphone {
    public static final float RANGE_MULTIPLIER = 3.0F;
    public static final float GAIN = 1.8F;
    private static final int DRAIN_PER_SECOND = 2;

    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(Megaphone::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ACTIVE.remove(handler.player.getUUID()));
        EntityTrackingEvents.START_TRACKING.register((entity, watcher) -> {
            if (entity instanceof ServerPlayer player) ServerPlayNetworking.send(watcher, new MegaphoneS2CPayload(player.getId(), isActive(player)));
        });
    }

    public static boolean isActive(UUID player) {
        return ACTIVE.contains(player);
    }

    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.contains(player.getUUID());
    }

    public static void start(ServerPlayer player) {
        if (!ACTIVE.add(player.getUUID())) return;
        broadcast(player, true);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 1.6F);
    }

    public static void stop(ServerPlayer player) {
        if (!ACTIVE.remove(player.getUUID())) return;
        broadcast(player, false);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    private static void broadcast(ServerPlayer player, boolean active) {
        MegaphoneS2CPayload payload = new MegaphoneS2CPayload(player.getId(), active);
        ServerPlayNetworking.send(player, payload);
        for (ServerPlayer watcher : PlayerLookup.tracking(player)) {
            if (watcher != player) ServerPlayNetworking.send(watcher, payload);
        }
    }

    private static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        boolean drainTick = server.getTickCount() % 20 == 0;
        for (UUID id : List.copyOf(ACTIVE)) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
                ACTIVE.remove(id);
                continue;
            }
            boolean valid = player.isAlive() && ArmedRingPower.isArmed(player) && !ArmedRingPower.isAbilityMode(player)
                    && ArmedRingPower.selectedConstruct(player).orElse(null) == RingPowerRegistry.MEGAPHONE;
            if (valid && drainTick && !player.hasInfiniteMaterials()) valid = PowerRingItem.consumeCharge(player, DRAIN_PER_SECOND);
            if (!valid) stop(player);
        }
    }

    private Megaphone() {}
}
