package dev.amble.core.light;

import dev.amble.config.BrightestDayConfig;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.networking.payloads.s2c.LightOrbS2CPayload;
import dev.amble.core.sculpt.SculptGeometry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class LightOrbManager {
    private static final int SPACING = 6;
    private static final int AIR_SEARCH = 2;
    private static final int WATCH_INTERVAL = 10;

    private static final List<Orb> ORBS = new ArrayList<>();
    private static int nextId;

    private static final class Orb {
        final int id;
        final ServerLevel level;
        final Vec3 center;
        final int size;
        final int color;
        final UUID caster;
        final int casterId;
        final long createdAt;
        final List<BlockPos> lights;
        final Set<ServerPlayer> watchers = Collections.newSetFromMap(new WeakHashMap<>());

        Orb(int id, ServerLevel level, Vec3 center, int size, int color, ServerPlayer caster, List<BlockPos> lights) {
            this.id = id;
            this.level = level;
            this.center = center;
            this.size = size;
            this.color = color;
            this.caster = caster.getUUID();
            this.casterId = caster.getId();
            this.createdAt = level.getGameTime();
            this.lights = lights;
        }
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(LightOrbManager::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ORBS.clear());
    }

    public static boolean place(ServerPlayer player, int size, int color) {
        ServerLevel level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 point = SculptGeometry.trace(level, player, eye, player.getLookAngle(), Double.NaN);
        if (point == null) return false;

        BlockPos center = LightManager.findAir(level, BlockPos.containing(point), AIR_SEARCH);
        if (center == null || !level.mayInteract(player, center)) return false;

        List<BlockPos> lights = new ArrayList<>();
        int steps = size - 1;
        for (int dx = -steps; dx <= steps; dx++) {
            for (int dy = -steps; dy <= steps; dy++) {
                for (int dz = -steps; dz <= steps; dz++) {
                    if (dx * dx + dy * dy + dz * dz > steps * steps) continue;
                    BlockPos candidate = center.offset(dx * SPACING, dy * SPACING, dz * SPACING);
                    BlockPos air = dx == 0 && dy == 0 && dz == 0 ? center : LightManager.findAir(level, candidate, AIR_SEARCH);
                    if (air == null || !level.mayInteract(player, air) || !LightManager.place(level, air)) continue;
                    lights.add(air.immutable());
                }
            }
        }
        if (lights.isEmpty()) return false;

        BrightestDayConfig config = BrightestDayConfig.get();
        while (count(player.getUUID()) >= Math.max(config.lightOrbMaxCount, 1)) {
            Orb oldest = oldest(player.getUUID());
            if (oldest == null) break;
            dissolve(oldest);
        }

        Orb orb = new Orb(nextId++, level, Vec3.atCenterOf(center), size, color, player, lights);
        ORBS.add(orb);
        syncWatchers(orb);
        level.playSound(null, center, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.6F);
        level.playSound(null, center, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.8F);
        return true;
    }

    public static long latestCreatedAt(UUID caster) {
        return ORBS.stream().filter(orb -> orb.caster.equals(caster)).mapToLong(orb -> orb.createdAt).max().orElse(Long.MIN_VALUE);
    }

    public static boolean dismissLatest(UUID caster) {
        Orb newest = newest(caster);
        if (newest == null) return false;
        dissolve(newest);
        return true;
    }

    public static void dismissAll(UUID caster) {
        for (Orb orb : List.copyOf(ORBS)) {
            if (orb.caster.equals(caster)) dissolve(orb);
        }
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 == 0) upkeep(server);
        if (server.getTickCount() % WATCH_INTERVAL == 0) ORBS.forEach(LightOrbManager::syncWatchers);
    }

    private static void upkeep(MinecraftServer server) {
        int perSize = BrightestDayConfig.get().lightOrbUpkeepPerSize;
        Map<UUID, Integer> totals = new HashMap<>();
        for (Orb orb : ORBS) totals.merge(orb.caster, orb.size * perSize, Integer::sum);

        for (Map.Entry<UUID, Integer> entry : totals.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.hasInfiniteMaterials() || entry.getValue() <= 0) continue;
            if (PowerRingItem.hasCharge(player) && PowerRingItem.drainWorn(player, entry.getValue())) continue;

            Orb newest = newest(entry.getKey());
            if (newest == null) continue;
            dissolve(newest);
            player.sendOverlayMessage(Component.translatable("message.brightestday.sculpt_fading"));
        }
    }

    private static int count(UUID caster) {
        return (int) ORBS.stream().filter(orb -> orb.caster.equals(caster)).count();
    }

    private static @Nullable Orb oldest(UUID caster) {
        return ORBS.stream().filter(orb -> orb.caster.equals(caster)).findFirst().orElse(null);
    }

    private static @Nullable Orb newest(UUID caster) {
        Orb newest = null;
        for (Orb orb : ORBS) {
            if (orb.caster.equals(caster)) newest = orb;
        }
        return newest;
    }

    private static void dissolve(Orb orb) {
        ORBS.remove(orb);
        for (BlockPos pos : orb.lights) LightManager.remove(orb.level, pos);
        LightOrbS2CPayload removal = new LightOrbS2CPayload(orb.id, orb.casterId, orb.center, orb.size, orb.color, false);
        for (ServerPlayer watcher : orb.watchers) {
            if (!watcher.hasDisconnected()) ServerPlayNetworking.send(watcher, removal);
        }
        orb.watchers.clear();
        orb.level.playSound(null, orb.center.x, orb.center.y, orb.center.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8F, 1.4F);
    }

    private static void syncWatchers(Orb orb) {
        Set<ServerPlayer> tracking = new HashSet<>(PlayerLookup.tracking(orb.level, BlockPos.containing(orb.center)));
        LightOrbS2CPayload spawn = new LightOrbS2CPayload(orb.id, orb.casterId, orb.center, orb.size, orb.color, true);
        for (ServerPlayer player : tracking) {
            if (orb.watchers.add(player)) ServerPlayNetworking.send(player, spawn);
        }
        LightOrbS2CPayload removal = new LightOrbS2CPayload(orb.id, orb.casterId, orb.center, orb.size, orb.color, false);
        orb.watchers.removeIf(player -> {
            boolean gone = !tracking.contains(player);
            if (gone && !player.hasDisconnected()) ServerPlayNetworking.send(player, removal);
            return gone;
        });
    }

    private LightOrbManager() {}
}
