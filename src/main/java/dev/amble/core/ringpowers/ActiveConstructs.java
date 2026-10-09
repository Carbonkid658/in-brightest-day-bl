package dev.amble.core.ringpowers;

import dev.amble.core.networking.payloads.s2c.ActiveConstructS2CPayload;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ActiveConstructs {
    private static final int RESYNC_INTERVAL = 40;

    private static final Map<UUID, Entry> SERVER = new HashMap<>();
    private static final Set<Integer> CLIENT = ConcurrentHashMap.newKeySet();

    private static final class Entry {
        ServerPlayer player;
        final Set<Object> tokens = new HashSet<>();

        Entry(ServerPlayer player) {
            this.player = player;
        }
    }

    public static void init() {
        ServerPlayerEvents.LEAVE.register(player -> SERVER.remove(player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SERVER.clear());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % RESYNC_INTERVAL != 0) return;
            for (Entry entry : List.copyOf(SERVER.values())) {
                ServerPlayer current = server.getPlayerList().getPlayer(entry.player.getUUID());
                if (current == null) continue;
                entry.player = current;
                broadcast(current, true);
            }
        });
    }

    public static void track(ServerPlayer owner, Object token) {
        Entry entry = SERVER.computeIfAbsent(owner.getUUID(), uuid -> new Entry(owner));
        entry.player = owner;
        boolean started = entry.tokens.isEmpty();
        entry.tokens.add(token);
        if (started) broadcast(owner, true);
    }

    public static void untrack(UUID owner, Object token) {
        Entry entry = SERVER.get(owner);
        if (entry == null || !entry.tokens.remove(token) || !entry.tokens.isEmpty()) return;
        SERVER.remove(owner);
        if (!entry.player.hasDisconnected()) broadcast(entry.player, false);
    }

    public static boolean hasActive(Player player) {
        if (player.level().isClientSide()) return CLIENT.contains(player.getId());
        return SERVER.containsKey(player.getUUID());
    }

    public static void setClient(int playerId, boolean active) {
        if (active) {
            CLIENT.add(playerId);
        } else {
            CLIENT.remove(playerId);
        }
    }

    public static void clearClient() {
        CLIENT.clear();
    }

    private static void broadcast(ServerPlayer player, boolean active) {
        ActiveConstructS2CPayload payload = new ActiveConstructS2CPayload(player.getId(), active);
        ServerPlayNetworking.send(player, payload);
        for (ServerPlayer watcher : PlayerLookup.tracking(player)) {
            if (watcher != player) ServerPlayNetworking.send(watcher, payload);
        }
    }

    private ActiveConstructs() {}
}
