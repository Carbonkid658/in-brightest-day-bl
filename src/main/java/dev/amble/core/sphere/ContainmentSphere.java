package dev.amble.core.sphere;

import dev.amble.config.BrightestDayConfig;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.mounts.ConstructMounts;
import dev.amble.core.networking.payloads.s2c.SphereS2CPayload;
import dev.amble.core.ringpowers.CorpsCombat;
import dev.amble.core.ringpowers.RingPowerRegistry;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import dev.amble.core.tractor.Struggle;
import dev.amble.core.tractor.TractorManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ContainmentSphere {
    public static final double GAP = 1.5;
    private static final double FOLLOW = 0.5;
    private static final double MAX_SPEED = 4.0;
    private static final double GRAVITY_COMPENSATION = 0.08;
    private static final double SNUG = 0.75;
    private static final int REFRESH_TICKS = 40;

    private record Held(Entity entity, Vec3 offset, Struggle struggle) {}

    private static final class Sphere {
        final ServerLevel level;
        final float radius;
        final int color;
        final List<Held> held = new ArrayList<>();
        Vec3 lastCenter;

        Sphere(ServerLevel level, float radius, int color, Vec3 center) {
            this.level = level;
            this.radius = radius;
            this.color = color;
            this.lastCenter = center;
        }
    }

    private static final Map<ServerPlayer, Sphere> SPHERES = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(ContainmentSphere::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> stop(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SPHERES.clear());
    }

    public static Vec3 center(ServerPlayer player, float radius) {
        return player.getEyePosition().add(player.getLookAngle().scale(radius + GAP));
    }

    public static boolean holds(Entity entity) {
        return SPHERES.values().stream().anyMatch(sphere -> sphere.held.stream().anyMatch(held -> held.entity() == entity));
    }

    public static void start(ServerPlayer player, float radius, int color) {
        stop(player);
        Vec3 center = center(player, radius);
        Sphere sphere = new Sphere(player.level(), radius, color, center);
        SPHERES.put(player, sphere);
        capture(player, sphere, center);
        broadcast(player, radius, color);
        player.level().playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.5F);
        player.level().playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 0.9F);
    }

    public static void stop(ServerPlayer player) {
        Sphere sphere = SPHERES.remove(player);
        if (sphere == null) return;
        broadcast(player, 0.0F, sphere.color);
        player.level().playSound(null, sphere.lastCenter.x, sphere.lastCenter.y, sphere.lastCenter.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    private static boolean capturable(ServerPlayer player, Entity entity) {
        if (entity == player || entity.isSpectator() || !entity.isAlive() || entity.isPassenger() || entity == player.getVehicle()) return false;
        if (ConstructMounts.isConstruct(entity) || holds(entity)) return false;
        return entity instanceof LivingEntity || entity instanceof ItemEntity || entity instanceof ExperienceOrb || entity instanceof FallingBlockEntity;
    }

    private static void capture(ServerPlayer player, Sphere sphere, Vec3 center) {
        double reach = sphere.radius;
        for (Entity entity : sphere.level.getEntities(player, new AABB(center, center).inflate(reach), entity -> capturable(player, entity))) {
            Vec3 offset = entity.getBoundingBox().getCenter().subtract(center);
            if (offset.length() > reach) continue;
            double room = Math.max(0.0, sphere.radius - Math.max(entity.getBbWidth(), entity.getBbHeight()) * 0.5) * SNUG;
            if (offset.length() > room) offset = offset.length() < 1.0E-4 ? Vec3.ZERO : offset.normalize().scale(room);
            sphere.held.add(new Held(entity, offset, new Struggle()));
        }
    }

    private static void tick(MinecraftServer server) {
        if (SPHERES.isEmpty()) return;
        boolean drainTick = server.getTickCount() % 20 == 0;
        boolean refresh = server.getTickCount() % REFRESH_TICKS == 0;

        for (ServerPlayer player : new ArrayList<>(SPHERES.keySet())) {
            Sphere sphere = SPHERES.get(player);
            boolean selected = ArmedRingPower.selectedConstruct(player).orElse(null) == RingPowerRegistry.CONTAINMENT_SPHERE;
            int drain = CorpsCombat.utilityCost(player, Math.max(1, Math.round(BrightestDayConfig.get().sphereDrainPerRadius * sphere.radius)));
            boolean outOfCharge = !PowerRingItem.hasCharge(player) || drainTick && !player.hasInfiniteMaterials() && !PowerRingItem.drainWorn(player, drain);
            if (player.isRemoved() || !player.isAlive() || player.level() != sphere.level || !ArmedRingPower.isArmed(player) || !selected || outOfCharge) {
                stop(player);
                continue;
            }

            Vec3 center = center(player, sphere.radius);
            Vec3 carried = center.subtract(sphere.lastCenter);
            sphere.lastCenter = center;
            capture(player, sphere, center);
            if (refresh) broadcast(player, sphere.radius, sphere.color);

            sphere.held.removeIf(held -> {
                Entity entity = held.entity();
                if (entity.isRemoved() || !entity.isAlive() || entity.level() != sphere.level || entity.isPassenger()) return true;
                if (held.struggle().escaped(entity)) {
                    TractorManager.breakFree(player, entity);
                    return true;
                }
                Vec3 goal = center.add(held.offset());
                Vec3 velocity = carried.add(goal.subtract(entity.getBoundingBox().getCenter()).scale(FOLLOW));
                if (velocity.length() > MAX_SPEED) velocity = velocity.normalize().scale(MAX_SPEED);
                if (!entity.isNoGravity()) velocity = velocity.add(0.0, GRAVITY_COMPENSATION, 0.0);
                held.struggle().push(entity, velocity);
                if (entity instanceof FallingBlockEntity block) block.time = 1;
                return false;
            });
        }
    }

    private static void broadcast(ServerPlayer player, float radius, int color) {
        SphereS2CPayload payload = new SphereS2CPayload(player.getId(), radius, color);
        ServerPlayNetworking.send(player, payload);
        for (ServerPlayer watcher : PlayerLookup.tracking(player)) {
            if (watcher != player) ServerPlayNetworking.send(watcher, payload);
        }
    }

    private ContainmentSphere() {}
}
