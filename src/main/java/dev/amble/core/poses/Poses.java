package dev.amble.core.poses;

import dev.amble.core.items.PowerRingItem;
import dev.amble.core.networking.payloads.s2c.PoseS2CPayload;
import dev.amble.core.ringpowers.RingPower;
import dev.amble.core.ringpowers.RingPowerRegistry;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Poses {
    public static final int MAX_STATE = 256;
    private static final int CHECK_INTERVAL = 5;

    private static final Map<UUID, Integer> POSES = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(Poses::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> POSES.remove(handler.player.getUUID()));
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (entity instanceof ServerPlayer player && damageTaken > 0.0F) set(player, 0);
        });
        EntityTrackingEvents.START_TRACKING.register((entity, watcher) -> {
            Integer pose = POSES.get(entity.getUUID());
            if (pose != null) ServerPlayNetworking.send(watcher, new PoseS2CPayload(entity.getId(), pose));
        });
    }

    public static boolean glowing(Player player) {
        Integer pose = POSES.get(player.getUUID());
        return pose != null && pose % 2 == 0;
    }

    public static boolean channeling(Player player) {
        if (!glowing(player)) return false;
        RingPower<?> selected = ArmedRingPower.selectedConstruct(player).orElse(null);
        return selected == RingPowerRegistry.BEAM || selected == RingPowerRegistry.HEAL_BEAM;
    }

    public static boolean canPose(ServerPlayer player) {
        return PowerRingItem.getWornCorps(player).isPresent() && !ArmedRingPower.isArmed(player) && player.isAlive() && !player.isSpectator();
    }

    public static void set(ServerPlayer player, int pose) {
        pose = Mth.clamp(pose, 0, MAX_STATE);
        if (pose > 0 && !canPose(player)) pose = 0;
        Integer previous = pose > 0 ? POSES.put(player.getUUID(), pose) : POSES.remove(player.getUUID());
        if (previous == null && pose == 0 || previous != null && previous == pose) return;
        broadcast(player, pose);
    }

    private static void broadcast(ServerPlayer player, int pose) {
        PoseS2CPayload payload = new PoseS2CPayload(player.getId(), pose);
        ServerPlayNetworking.send(player, payload);
        for (ServerPlayer watcher : PlayerLookup.tracking(player)) {
            if (watcher != player) ServerPlayNetworking.send(watcher, payload);
        }
    }

    private static void tick(MinecraftServer server) {
        if (POSES.isEmpty() || server.getTickCount() % CHECK_INTERVAL != 0) return;
        for (UUID id : Map.copyOf(POSES).keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) POSES.remove(id);
            else if (!canPose(player)) set(player, 0);
        }
    }

    private Poses() {}
}
