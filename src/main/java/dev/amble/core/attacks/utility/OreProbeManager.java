package dev.amble.core.attacks.utility;

import dev.amble.core.networking.payloads.s2c.OreProbeS2CPayload;
import dev.amble.core.ringpowers.ActiveConstructs;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class OreProbeManager {
    private static final int LIFETIME_TICKS = 600;
    private static final int WATCH_INTERVAL = 10;

    private static final List<Probe> PROBES = new ArrayList<>();
    private static int nextId;

    private static final class Probe {
        final int id;
        final UUID owner;
        final int ownerId;
        final int color;
        final ServerLevel level;
        final long createdAt;
        final long expiresAt;
        final Set<ServerPlayer> watchers = Collections.newSetFromMap(new WeakHashMap<>());

        Probe(int id, ServerPlayer owner, int color) {
            this.id = id;
            this.owner = owner.getUUID();
            this.ownerId = owner.getId();
            this.color = color;
            this.level = owner.level();
            this.createdAt = this.level.getGameTime();
            this.expiresAt = this.createdAt + LIFETIME_TICKS;
        }

        int remaining() {
            return (int) Math.max(this.expiresAt - this.level.getGameTime(), 0L);
        }
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(OreProbeManager::tick);
        ServerPlayerEvents.LEAVE.register(player -> dismissAll(player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PROBES.clear());
    }

    public static void launch(ServerPlayer player, int color) {
        dismissAll(player.getUUID());
        Probe probe = new Probe(nextId++, player, color);
        PROBES.add(probe);
        ActiveConstructs.track(player, probe);
        syncWatchers(probe, player);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 2.0F);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    public static long latestCreatedAt(UUID owner) {
        return PROBES.stream().filter(probe -> probe.owner.equals(owner)).mapToLong(probe -> probe.createdAt).max().orElse(Long.MIN_VALUE);
    }

    public static boolean dismissLatest(UUID owner) {
        Probe newest = null;
        for (Probe probe : PROBES) {
            if (probe.owner.equals(owner)) newest = probe;
        }
        if (newest == null) return false;
        dissolve(newest);
        return true;
    }

    public static void dismissAll(UUID owner) {
        for (Probe probe : List.copyOf(PROBES)) {
            if (probe.owner.equals(owner)) dissolve(probe);
        }
    }

    private static void tick(MinecraftServer server) {
        boolean sync = server.getTickCount() % WATCH_INTERVAL == 0;
        for (Probe probe : List.copyOf(PROBES)) {
            ServerPlayer owner = server.getPlayerList().getPlayer(probe.owner);
            if (owner == null || owner.getId() != probe.ownerId || owner.level() != probe.level || !owner.isAlive() || probe.level.getGameTime() >= probe.expiresAt) {
                dissolve(probe);
                continue;
            }
            if (sync) syncWatchers(probe, owner);
        }
    }

    private static void dissolve(Probe probe) {
        if (!PROBES.remove(probe)) return;
        ActiveConstructs.untrack(probe.owner, probe);
        OreProbeS2CPayload removal = new OreProbeS2CPayload(probe.id, probe.ownerId, probe.color, 0, false);
        for (ServerPlayer watcher : probe.watchers) {
            if (!watcher.hasDisconnected()) ServerPlayNetworking.send(watcher, removal);
        }
        probe.watchers.clear();
        ServerPlayer owner = probe.level.getServer().getPlayerList().getPlayer(probe.owner);
        if (owner != null && owner.level() == probe.level) {
            probe.level.playSound(null, owner.getX(), owner.getEyeY(), owner.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.6F, 1.6F);
        }
    }

    private static void syncWatchers(Probe probe, ServerPlayer owner) {
        Set<ServerPlayer> tracking = new HashSet<>(PlayerLookup.tracking(owner));
        tracking.add(owner);
        OreProbeS2CPayload spawn = new OreProbeS2CPayload(probe.id, probe.ownerId, probe.color, probe.remaining(), true);
        for (ServerPlayer player : tracking) {
            if (probe.watchers.add(player)) ServerPlayNetworking.send(player, spawn);
        }
        OreProbeS2CPayload removal = new OreProbeS2CPayload(probe.id, probe.ownerId, probe.color, 0, false);
        probe.watchers.removeIf(player -> {
            boolean gone = !tracking.contains(player);
            if (gone && !player.hasDisconnected()) ServerPlayNetworking.send(player, removal);
            return gone;
        });
    }

    private OreProbeManager() {}
}
