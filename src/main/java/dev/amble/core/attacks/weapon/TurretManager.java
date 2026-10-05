package dev.amble.core.attacks.weapon;

import dev.amble.core.ringpowers.ActiveConstructs;
import dev.amble.core.team.RingDamage;
import dev.amble.config.BrightestDayConfig;
import dev.amble.core.networking.payloads.s2c.TurretBoltS2CPayload;
import dev.amble.core.networking.payloads.s2c.TurretS2CPayload;
import dev.amble.core.ringpowers.constructs.ConstructRingPower;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class TurretManager {
    private static final double PLACE_RANGE = 6.0;
    private static final double PLACE_BACKOFF = 1.0;
    private static final double HOVER_HEIGHT = 0.6;
    private static final double TURRET_HALF = 0.4;
    private static final int FIRST_SHOT_DELAY = 10;
    private static final float BOLT_SPEED = 1.1F;
    private static final double BOLT_RADIUS = 0.3;
    private static final double MUZZLE_OFFSET = 0.5;
    private static final int BOLT_LIFETIME = 40;
    private static final int WATCH_INTERVAL = 10;

    private static final List<Turret> TURRETS = new ArrayList<>();
    private static final List<Bolt> BOLTS = new ArrayList<>();
    private static int nextId;

    private static final class Turret {
        final int id;
        final ServerLevel level;
        final Vec3 center;
        final int color;
        final UUID owner;
        final int ownerId;
        final long createdAt;
        final long expiresAt;
        long nextShot;
        final Set<ServerPlayer> watchers = Collections.newSetFromMap(new WeakHashMap<>());

        Turret(int id, ServerLevel level, Vec3 center, int color, ServerPlayer owner) {
            this.id = id;
            this.level = level;
            this.center = center;
            this.color = color;
            this.owner = owner.getUUID();
            this.ownerId = owner.getId();
            this.createdAt = level.getGameTime();
            this.expiresAt = this.createdAt + BrightestDayConfig.get().turretLifetimeTicks;
            this.nextShot = this.createdAt + FIRST_SHOT_DELAY;
        }

        int remaining() {
            return (int) Math.max(this.expiresAt - this.level.getGameTime(), 0L);
        }
    }

    private static final class Bolt {
        final ServerLevel level;
        final UUID owner;
        final int targetId;
        Vec3 pos;
        int age;

        Bolt(ServerLevel level, UUID owner, int targetId, Vec3 pos) {
            this.level = level;
            this.owner = owner;
            this.targetId = targetId;
            this.pos = pos;
        }
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(TurretManager::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            TURRETS.clear();
            BOLTS.clear();
        });
    }

    public static boolean place(ServerPlayer player, int color) {
        ServerLevel level = player.level();
        ConstructRingPower.Aim aim = ConstructRingPower.aim(player, PLACE_RANGE);
        double distance = aim.eye().distanceTo(aim.end());
        Vec3 point = aim.eye().add(aim.look().scale(Math.max(distance - PLACE_BACKOFF, 1.0))).add(0.0, HOVER_HEIGHT, 0.0);
        if (!level.noCollision(new AABB(point, point).inflate(TURRET_HALF))) point = point.subtract(0.0, HOVER_HEIGHT, 0.0);
        if (!level.noCollision(new AABB(point, point).inflate(TURRET_HALF)) || !level.mayInteract(player, BlockPos.containing(point))) return false;

        while (count(player.getUUID()) >= Math.max(BrightestDayConfig.get().turretMaxCount, 1)) {
            Turret oldest = oldest(player.getUUID());
            if (oldest == null) break;
            dissolve(oldest);
        }

        Turret turret = new Turret(nextId++, level, point, color, player);
        TURRETS.add(turret);
        ActiveConstructs.track(player, turret);
        syncWatchers(turret);
        level.playSound(null, point.x, point.y, point.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7F, 1.8F);
        level.playSound(null, point.x, point.y, point.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        return true;
    }

    public static long latestCreatedAt(UUID owner) {
        return TURRETS.stream().filter(turret -> turret.owner.equals(owner)).mapToLong(turret -> turret.createdAt).max().orElse(Long.MIN_VALUE);
    }

    public static boolean dismissLatest(UUID owner) {
        Turret newest = newest(owner);
        if (newest == null) return false;
        dissolve(newest);
        return true;
    }

    public static void dismissAll(UUID owner) {
        for (Turret turret : List.copyOf(TURRETS)) {
            if (turret.owner.equals(owner)) dissolve(turret);
        }
    }

    private static void tick(MinecraftServer server) {
        for (Turret turret : List.copyOf(TURRETS)) {
            if (server.getPlayerList().getPlayer(turret.owner) == null || turret.level.getGameTime() >= turret.expiresAt) {
                dissolve(turret);
                continue;
            }
            if (turret.level.getGameTime() >= turret.nextShot) shoot(turret);
        }
        if (server.getTickCount() % WATCH_INTERVAL == 0) TURRETS.forEach(TurretManager::syncWatchers);
        tickBolts(server);
    }

    private static void shoot(Turret turret) {
        LivingEntity target = findTarget(turret);
        if (target == null) {
            turret.nextShot = turret.level.getGameTime() + 5;
            return;
        }
        turret.nextShot = turret.level.getGameTime() + Math.max(BrightestDayConfig.get().turretFireInterval, 1);

        Vec3 aim = target.getBoundingBox().getCenter().subtract(turret.center).normalize();
        Vec3 muzzle = turret.center.add(aim.scale(MUZZLE_OFFSET));
        BOLTS.add(new Bolt(turret.level, turret.owner, target.getId(), muzzle));
        turret.level.playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.8F);
        turret.level.playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.SHULKER_SHOOT, SoundSource.PLAYERS, 0.5F, 1.6F);

        TurretBoltS2CPayload payload = new TurretBoltS2CPayload(turret.id, muzzle, target.getId(), turret.color, BOLT_SPEED);
        for (ServerPlayer watcher : turret.watchers) {
            if (!watcher.hasDisconnected()) ServerPlayNetworking.send(watcher, payload);
        }
    }

    private static @Nullable LivingEntity findTarget(Turret turret) {
        ServerPlayer owner = turret.level.getServer().getPlayerList().getPlayer(turret.owner);
        double range = BrightestDayConfig.get().turretRange;
        LivingEntity best = null;
        double bestDistance = range * range;
        for (LivingEntity entity : turret.level.getEntitiesOfClass(LivingEntity.class, new AABB(turret.center, turret.center).inflate(range),
                entity -> entity instanceof Enemy && entity.isAlive() && !entity.isSpectator() && (owner == null || !owner.isAlliedTo(entity)))) {
            Vec3 center = entity.getBoundingBox().getCenter();
            double distance = center.distanceToSqr(turret.center);
            if (distance > bestDistance || !clear(turret.level, turret.center, center)) continue;
            best = entity;
            bestDistance = distance;
        }
        return best;
    }

    private static boolean clear(ServerLevel level, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty())).getType() == HitResult.Type.MISS;
    }

    private static void tickBolts(MinecraftServer server) {
        Iterator<Bolt> iterator = BOLTS.iterator();
        while (iterator.hasNext()) {
            Bolt bolt = iterator.next();
            Entity target = bolt.level.getEntity(bolt.targetId);
            if (++bolt.age > BOLT_LIFETIME || !(target instanceof LivingEntity living) || !living.isAlive()) {
                iterator.remove();
                continue;
            }

            Vec3 to = living.getBoundingBox().getCenter().subtract(bolt.pos);
            double distance = to.length();
            if (distance <= BOLT_SPEED + BOLT_RADIUS + living.getBbWidth() * 0.5) {
                ServerPlayer owner = server.getPlayerList().getPlayer(bolt.owner);
                DamageSource source = owner != null ? RingDamage.source(bolt.level, owner) : bolt.level.damageSources().magic();
                living.hurtServer(bolt.level, source, BrightestDayConfig.get().turretBoltDamage);
                Vec3 hit = living.getBoundingBox().getCenter();
                bolt.level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8F, 1.7F);
                iterator.remove();
                continue;
            }

            Vec3 next = bolt.pos.add(to.scale(BOLT_SPEED / distance));
            if (!clear(bolt.level, bolt.pos, next)) {
                iterator.remove();
                continue;
            }
            bolt.pos = next;
        }
    }

    private static int count(UUID owner) {
        return (int) TURRETS.stream().filter(turret -> turret.owner.equals(owner)).count();
    }

    private static @Nullable Turret oldest(UUID owner) {
        return TURRETS.stream().filter(turret -> turret.owner.equals(owner)).findFirst().orElse(null);
    }

    private static @Nullable Turret newest(UUID owner) {
        Turret newest = null;
        for (Turret turret : TURRETS) {
            if (turret.owner.equals(owner)) newest = turret;
        }
        return newest;
    }

    private static void dissolve(Turret turret) {
        TURRETS.remove(turret);
        ActiveConstructs.untrack(turret.owner, turret);
        TurretS2CPayload removal = new TurretS2CPayload(turret.id, turret.ownerId, turret.center, turret.color, 0, false);
        for (ServerPlayer watcher : turret.watchers) {
            if (!watcher.hasDisconnected()) ServerPlayNetworking.send(watcher, removal);
        }
        turret.watchers.clear();
        turret.level.playSound(null, turret.center.x, turret.center.y, turret.center.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8F, 1.4F);
    }

    private static void syncWatchers(Turret turret) {
        Set<ServerPlayer> tracking = new HashSet<>(PlayerLookup.tracking(turret.level, BlockPos.containing(turret.center)));
        TurretS2CPayload spawn = new TurretS2CPayload(turret.id, turret.ownerId, turret.center, turret.color, turret.remaining(), true);
        for (ServerPlayer player : tracking) {
            if (turret.watchers.add(player)) ServerPlayNetworking.send(player, spawn);
        }
        TurretS2CPayload removal = new TurretS2CPayload(turret.id, turret.ownerId, turret.center, turret.color, 0, false);
        turret.watchers.removeIf(player -> {
            boolean gone = !tracking.contains(player);
            if (gone && !player.hasDisconnected()) ServerPlayNetworking.send(player, removal);
            return gone;
        });
    }

    private TurretManager() {}
}
