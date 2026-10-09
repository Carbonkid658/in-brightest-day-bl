package dev.amble.core.glide;

import dev.amble.config.BrightestDayConfig;
import dev.amble.core.networking.payloads.s2c.GlideS2CPayload;
import dev.amble.core.ringpowers.impl.FlightRingPower;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class GlideManager {
    private static final int WATCH_INTERVAL = 10;

    private static final Map<UUID, Buff> BUFFS = new HashMap<>();
    private static final Set<Integer> CLIENT = new HashSet<>();

    private static final class Buff {
        final LivingEntity entity;
        final Level level;
        UUID caster;
        int casterId;
        long createdAt;
        int color;
        int remaining;
        final Set<ServerPlayer> watchers = Collections.newSetFromMap(new WeakHashMap<>());

        Buff(LivingEntity entity, ServerPlayer caster, int color, int remaining) {
            this.entity = entity;
            this.level = entity.level();
            this.caster = caster.getUUID();
            this.casterId = caster.getId();
            this.createdAt = this.level.getGameTime();
            this.color = color;
            this.remaining = remaining;
        }

        GlideS2CPayload payload(boolean present) {
            return new GlideS2CPayload(this.entity.getId(), this.casterId, this.color, this.remaining, present);
        }
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(GlideManager::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> end(handler.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> BUFFS.clear());
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(source.is(DamageTypeTags.IS_FALL) && isGliding(entity)));
    }

    public static void apply(LivingEntity target, ServerPlayer caster, int color, int duration) {
        Buff buff = BUFFS.get(target.getUUID());
        if (buff != null && buff.entity == target) {
            buff.caster = caster.getUUID();
            buff.casterId = caster.getId();
            buff.createdAt = buff.level.getGameTime();
            buff.color = color;
            buff.remaining = Math.max(buff.remaining, duration);
            GlideS2CPayload refresh = buff.payload(true);
            for (ServerPlayer watcher : buff.watchers) {
                if (!watcher.hasDisconnected()) ServerPlayNetworking.send(watcher, refresh);
            }
        } else {
            if (buff != null) end(target.getUUID());
            buff = new Buff(target, caster, color, duration);
            BUFFS.put(target.getUUID(), buff);
        }
        target.resetFallDistance();
        syncWatchers(buff);
    }

    public static long latestCreatedAt(UUID caster) {
        return BUFFS.values().stream().filter(buff -> buff.caster.equals(caster)).mapToLong(buff -> buff.createdAt).max().orElse(Long.MIN_VALUE);
    }

    public static boolean dismissLatest(UUID caster) {
        Buff newest = newest(caster);
        if (newest == null) return false;
        dismiss(newest);
        return true;
    }

    public static void dismissAll(UUID caster) {
        for (Buff buff : List.copyOf(BUFFS.values())) {
            if (buff.caster.equals(caster)) dismiss(buff);
        }
    }

    public static boolean isGliding(Entity entity) {
        if (entity.level().isClientSide()) return CLIENT.contains(entity.getId());
        Buff buff = BUFFS.get(entity.getUUID());
        return buff != null && buff.entity == entity;
    }

    public static boolean grantsGlide(LivingEntity entity) {
        return entity instanceof Player player && isGliding(player) && !FlightRingPower.canFly(player);
    }

    public static void setClient(int entityId, boolean present) {
        if (present) CLIENT.add(entityId);
        else CLIENT.remove(entityId);
    }

    public static void clearClient() {
        CLIENT.clear();
    }

    private static void tick(MinecraftServer server) {
        boolean sync = server.getTickCount() % WATCH_INTERVAL == 0;
        Iterator<Map.Entry<UUID, Buff>> iterator = BUFFS.entrySet().iterator();
        while (iterator.hasNext()) {
            Buff buff = iterator.next().getValue();
            LivingEntity entity = buff.entity;
            boolean ringFlying = entity instanceof Player player && FlightRingPower.isFlying(player);
            if (--buff.remaining <= 0 || ringFlying || entity.isRemoved() || !entity.isAlive() || entity.level() != buff.level) {
                iterator.remove();
                dissolve(buff);
                continue;
            }

            if (!(entity instanceof Player)) drift(entity);
            if (sync) syncWatchers(buff);
        }
    }

    private static void drift(LivingEntity entity) {
        entity.resetFallDistance();
        if (entity.onGround() || entity.isInWater() || entity.isPassenger()) return;
        Vec3 movement = entity.getDeltaMovement();
        if (movement.y >= -BrightestDayConfig.get().gliderMobFallSpeed) return;
        entity.setDeltaMovement(movement.x, -BrightestDayConfig.get().gliderMobFallSpeed, movement.z);
    }

    private static @Nullable Buff newest(UUID caster) {
        Buff newest = null;
        for (Buff buff : BUFFS.values()) {
            if (buff.caster.equals(caster) && (newest == null || buff.createdAt >= newest.createdAt)) newest = buff;
        }
        return newest;
    }

    private static void dismiss(Buff buff) {
        BUFFS.remove(buff.entity.getUUID());
        dissolve(buff);
        buff.level.playSound(null, buff.entity.getX(), buff.entity.getY(), buff.entity.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8F, 1.4F);
    }

    private static void end(UUID id) {
        Buff buff = BUFFS.remove(id);
        if (buff != null) dissolve(buff);
    }

    private static void dissolve(Buff buff) {
        buff.entity.resetFallDistance();
        GlideS2CPayload removal = buff.payload(false);
        for (ServerPlayer watcher : buff.watchers) {
            if (!watcher.hasDisconnected()) ServerPlayNetworking.send(watcher, removal);
        }
        buff.watchers.clear();
    }

    private static void syncWatchers(Buff buff) {
        Set<ServerPlayer> tracking = new HashSet<>(PlayerLookup.tracking(buff.entity));
        if (buff.entity instanceof ServerPlayer self) tracking.add(self);
        GlideS2CPayload spawn = buff.payload(true);
        for (ServerPlayer player : tracking) {
            if (buff.watchers.add(player)) ServerPlayNetworking.send(player, spawn);
        }
        GlideS2CPayload removal = buff.payload(false);
        buff.watchers.removeIf(player -> {
            boolean gone = !tracking.contains(player);
            if (gone && !player.hasDisconnected()) ServerPlayNetworking.send(player, removal);
            return gone;
        });
    }

    private GlideManager() {}
}
