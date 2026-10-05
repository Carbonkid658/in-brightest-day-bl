package dev.amble.core.attacks.utility;

import dev.amble.core.ringpowers.ActiveConstructs;
import dev.amble.core.team.RingDamage;
import dev.amble.core.attacks.projectile.ProjectileTargeting;
import dev.amble.core.networking.payloads.s2c.GrappleS2CPayload;
import dev.amble.core.ringpowers.constructs.ConstructRingPower;
import dev.amble.core.ringpowers.impl.FlightRingPower;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GrappleManager {
    private static final double MAX_RANGE = 40.0;
    private static final double BREAK_SLACK = 6.0;
    private static final double HOOK_SPEED = 3.2;
    private static final double RETRACT_SPEED = 3.6;
    private static final double HIT_PADDING = 0.4;
    private static final double PULL_SPEED = 1.35;
    private static final double PULL_BLEND = 0.35;
    private static final double PULL_LIFT = 0.12;
    private static final double ARRIVE_DISTANCE = 1.5;
    private static final double ARRIVE_DAMPING = 0.4;
    private static final int PULL_TIMEOUT = 60;
    private static final int SAFE_FALL_TICKS = 40;
    private static final double ENTITY_PULL_SPEED = 1.1;
    private static final double ENTITY_LIFT = 0.12;
    private static final double ENTITY_STOP_DISTANCE = 2.5;
    private static final double ENTITY_DAMPING = 0.3;
    private static final int ENTITY_REEL_TICKS = 8;
    private static final float ENTITY_DAMAGE = 1.0F;
    private static final int REEL_SOUND_INTERVAL = 5;

    private static final Map<ServerPlayer, Hook> HOOKS = new HashMap<>();
    private static final Map<UUID, Integer> SAFE_FALL = new HashMap<>();
    private static int nextId;
    private static int ticks;

    private enum Phase {
        FLYING,
        ANCHORED,
        REELING,
        RETRACTING
    }

    private static final class Hook {
        final int id;
        final ServerPlayer owner;
        final ServerLevel level;
        final int color;
        final Vec3 direction;
        Vec3 position;
        double travelled;
        Phase phase = Phase.FLYING;
        @Nullable BlockPos anchorBlock;
        @Nullable Entity target;
        boolean latched;
        int phaseAge;

        Hook(int id, ServerPlayer owner, int color, Vec3 origin, Vec3 direction) {
            this.id = id;
            this.owner = owner;
            this.level = owner.level();
            this.color = color;
            this.position = origin;
            this.direction = direction;
        }
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(GrappleManager::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            clear(handler.player);
            SAFE_FALL.remove(handler.player.getUUID());
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(entity instanceof ServerPlayer player && source.is(DamageTypeTags.IS_FALL) && isFallSafe(player)));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            HOOKS.clear();
            SAFE_FALL.clear();
        });
    }

    public static boolean isActive(ServerPlayer player) {
        return HOOKS.containsKey(player);
    }

    public static boolean isLatched(ServerPlayer player) {
        Hook hook = HOOKS.get(player);
        return hook != null && hook.latched;
    }

    public static void launch(ServerPlayer player, int color) {
        if (HOOKS.containsKey(player)) return;
        Vec3 origin = ProjectileTargeting.hand(player);
        ConstructRingPower.Aim aim = ConstructRingPower.aim(player, MAX_RANGE);
        Vec3 toAim = aim.end().subtract(origin);
        Vec3 direction = toAim.lengthSqr() < 1.0E-4 ? aim.look() : toAim.normalize();
        Hook hook = new Hook(nextId++, player, color, origin, direction);
        HOOKS.put(player, hook);
        ActiveConstructs.track(player, hook);
        send(hook);
    }

    public static void release(ServerPlayer player) {
        Hook hook = HOOKS.get(player);
        if (hook == null || hook.phase == Phase.RETRACTING) return;
        retract(hook);
        send(hook);
    }

    public static void dismiss(ServerPlayer owner) {
        Hook hook = HOOKS.remove(owner);
        if (hook == null) return;
        detach(hook);
        broadcast(hook, GrappleS2CPayload.GONE);
    }

    private static void clear(ServerPlayer player) {
        Hook hook = HOOKS.remove(player);
        if (hook != null) detach(hook);
    }

    private static boolean isFallSafe(ServerPlayer player) {
        Hook hook = HOOKS.get(player);
        if (hook != null && hook.phase == Phase.ANCHORED) return true;
        Integer until = SAFE_FALL.get(player.getUUID());
        return until != null && until >= ticks;
    }

    private static void tick(MinecraftServer server) {
        ticks++;
        SAFE_FALL.values().removeIf(until -> until < ticks);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (SAFE_FALL.containsKey(player.getUUID())) player.resetFallDistance();
        }

        for (ServerPlayer owner : new ArrayList<>(HOOKS.keySet())) {
            Hook hook = HOOKS.get(owner);
            if (owner.isRemoved() || !owner.isAlive() || owner.isSpectator() || owner.level() != hook.level) {
                HOOKS.remove(owner);
                detach(hook);
                broadcast(hook, GrappleS2CPayload.GONE);
                continue;
            }

            hook.phaseAge++;
            boolean done = switch (hook.phase) {
                case FLYING -> fly(hook);
                case ANCHORED -> anchor(hook);
                case REELING -> reel(hook);
                case RETRACTING -> returnHome(hook);
            };
            if (done) {
                HOOKS.remove(owner);
                detach(hook);
                broadcast(hook, GrappleS2CPayload.GONE);
            } else {
                send(hook);
            }
        }
    }

    private static boolean fly(Hook hook) {
        ServerPlayer owner = hook.owner;
        ServerLevel level = hook.level;
        double step = Math.min(HOOK_SPEED, MAX_RANGE - hook.travelled);
        Vec3 from = hook.position;
        Vec3 to = from.add(hook.direction.scale(step));

        BlockHitResult blockHit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        boolean blocked = blockHit.getType() == HitResult.Type.BLOCK;
        if (blocked) to = blockHit.getLocation();

        Entity struck = firstEntity(owner, from, to);
        if (struck != null) {
            hook.position = struck.getBoundingBox().getCenter();
            latchEntity(hook, struck);
            return false;
        }
        if (blocked) {
            hook.position = to;
            latchBlock(hook, blockHit.getBlockPos());
            return false;
        }

        hook.position = to;
        hook.travelled += step;
        if (hook.travelled >= MAX_RANGE - 1.0E-3) retract(hook);
        return false;
    }

    private static @Nullable Entity firstEntity(ServerPlayer owner, Vec3 from, Vec3 to) {
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : owner.level().getEntities(owner, new AABB(from, to).inflate(HIT_PADDING),
                entity -> entity instanceof LivingEntity && entity.isAlive() && entity.isPickable() && !entity.isSpectator() && !entity.isPassengerOfSameVehicle(owner))) {
            AABB box = entity.getBoundingBox().inflate(HIT_PADDING);
            if (!box.contains(from) && box.clip(from, to).isEmpty()) continue;
            double distance = entity.getBoundingBox().getCenter().distanceToSqr(from);
            if (distance >= bestDistance) continue;
            best = entity;
            bestDistance = distance;
        }
        return best;
    }

    private static void latchBlock(Hook hook, BlockPos pos) {
        ServerPlayer owner = hook.owner;
        hook.anchorBlock = pos;
        hook.phase = Phase.ANCHORED;
        hook.phaseAge = 0;
        attach(hook);

        if (FlightRingPower.canFly(owner)) FlightRingPower.setEnabled(owner, false);
        if (owner.isFallFlying()) owner.stopFallFlying();
        owner.resetFallDistance();

        Vec3 at = hook.position;
        hook.level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.9F, 1.5F);
        hook.level.playSound(null, at.x, at.y, at.z, SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    private static void latchEntity(Hook hook, Entity target) {
        ServerPlayer owner = hook.owner;
        hook.target = target;
        hook.phase = Phase.REELING;
        hook.phaseAge = 0;
        attach(hook);

        if (ProjectileTargeting.isTarget(owner, target) && target instanceof LivingEntity living) {
            living.hurtServer(hook.level, RingDamage.source(hook.level, owner), ENTITY_DAMAGE);
        }
        Vec3 at = hook.position;
        hook.level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.9F, 1.3F);
        hook.level.playSound(null, at.x, at.y, at.z, SoundEvents.LEAD_TIED, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static boolean anchor(Hook hook) {
        ServerPlayer owner = hook.owner;
        ServerLevel level = hook.level;
        BlockPos pos = hook.anchorBlock;
        if (pos == null || !level.isLoaded(pos) || level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            snap(hook);
            return false;
        }

        Vec3 center = owner.position().add(0.0, owner.getBbHeight() * 0.5, 0.0);
        Vec3 toAnchor = hook.position.subtract(center);
        double distance = toAnchor.length();
        if (distance > MAX_RANGE + BREAK_SLACK) {
            snap(hook);
            return false;
        }
        if (distance < ARRIVE_DISTANCE) {
            setVelocity(owner, owner.getDeltaMovement().scale(ARRIVE_DAMPING));
            retract(hook);
            return false;
        }
        if (owner.isShiftKeyDown() || hook.phaseAge > PULL_TIMEOUT) {
            retract(hook);
            return false;
        }

        Vec3 desired = toAnchor.scale(1.0 / distance).scale(PULL_SPEED).add(0.0, PULL_LIFT, 0.0);
        Vec3 velocity = owner.getDeltaMovement().lerp(desired, PULL_BLEND);
        if (velocity.length() > distance) velocity = velocity.normalize().scale(distance);
        setVelocity(owner, velocity);
        owner.resetFallDistance();

        if (hook.phaseAge % REEL_SOUND_INTERVAL == 0) {
            level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.6F, 1.4F);
        }
        return false;
    }

    private static boolean reel(Hook hook) {
        ServerPlayer owner = hook.owner;
        Entity target = hook.target;
        if (target == null || target.isRemoved() || !target.isAlive() || target.level() != hook.level) {
            retract(hook);
            return false;
        }

        Vec3 center = target.getBoundingBox().getCenter();
        hook.position = center;
        Vec3 toOwner = ProjectileTargeting.hand(owner).subtract(center);
        double distance = toOwner.length();
        if (distance > MAX_RANGE + BREAK_SLACK) {
            snap(hook);
            return false;
        }
        if (distance < ENTITY_STOP_DISTANCE || hook.phaseAge > ENTITY_REEL_TICKS) {
            setVelocity(target, target.getDeltaMovement().scale(ENTITY_DAMPING));
            retract(hook);
            return false;
        }

        Vec3 velocity = toOwner.scale(1.0 / distance).scale(Math.min(ENTITY_PULL_SPEED, distance - ENTITY_STOP_DISTANCE * 0.5)).add(0.0, ENTITY_LIFT, 0.0);
        setVelocity(target, velocity);
        target.resetFallDistance();
        return false;
    }

    private static boolean returnHome(Hook hook) {
        Vec3 hand = ProjectileTargeting.hand(hook.owner);
        Vec3 toHand = hand.subtract(hook.position);
        double distance = toHand.length();
        if (distance <= RETRACT_SPEED) return true;
        hook.position = hook.position.add(toHand.scale(RETRACT_SPEED / distance));
        return false;
    }

    private static void snap(Hook hook) {
        Vec3 at = hook.position;
        hook.level.playSound(null, at.x, at.y, at.z, SoundEvents.LEAD_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
        retract(hook);
    }

    private static void retract(Hook hook) {
        detach(hook);
        hook.phase = Phase.RETRACTING;
        hook.phaseAge = 0;
        hook.target = null;
        hook.anchorBlock = null;
    }

    private static void attach(Hook hook) {
        hook.latched = true;
    }

    private static void detach(Hook hook) {
        ActiveConstructs.untrack(hook.owner.getUUID(), hook);
        if (!hook.latched) return;
        hook.latched = false;
        if (hook.phase == Phase.ANCHORED) {
            SAFE_FALL.put(hook.owner.getUUID(), ticks + SAFE_FALL_TICKS);
            hook.owner.resetFallDistance();
            hook.level.playSound(null, hook.owner.getX(), hook.owner.getY(), hook.owner.getZ(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.7F, 1.5F);
        }
    }

    private static void setVelocity(Entity entity, Vec3 velocity) {
        entity.setDeltaMovement(velocity);
        entity.needsSync = true;
        entity.syncVelocity = true;
    }

    private static void send(Hook hook) {
        int state = switch (hook.phase) {
            case FLYING -> GrappleS2CPayload.FLYING;
            case ANCHORED, REELING -> GrappleS2CPayload.LATCHED;
            case RETRACTING -> GrappleS2CPayload.RETRACTING;
        };
        broadcast(hook, state);
    }

    private static void broadcast(Hook hook, int state) {
        int targetId = hook.target != null ? hook.target.getId() : GrappleS2CPayload.NO_TARGET;
        ProjectileTargeting.broadcast(hook.owner, new GrappleS2CPayload(hook.id, hook.owner.getId(), hook.color, hook.position, state, targetId));
    }

    private GrappleManager() {}
}
