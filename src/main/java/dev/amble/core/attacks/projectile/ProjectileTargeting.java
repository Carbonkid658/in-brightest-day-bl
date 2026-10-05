package dev.amble.core.attacks.projectile;

import dev.amble.core.team.RingDamage;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class ProjectileTargeting {
    private static final float SHOULDER_HEIGHT = 1.4F;
    private static final float SHOULDER_OFFSET = 0.35F;
    private static final float ARM_LENGTH = 0.7F;

    public static Vec3 hand(Player player) {
        float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0.0, -Mth.sin(yaw));
        float side = player.getMainArm() == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        return player.position()
                .add(0.0, SHOULDER_HEIGHT, 0.0)
                .add(right.scale(side * SHOULDER_OFFSET))
                .add(player.getLookAngle().scale(ARM_LENGTH));
    }

    public static boolean isTarget(ServerPlayer player, Entity entity) {
        return entity != player && entity instanceof LivingEntity && entity.isAlive() && entity.isPickable()
                && !entity.isSpectator() && !player.isAlliedTo(entity);
    }

    public static @Nullable Entity seek(ServerPlayer player, Vec3 eye, Vec3 look, double range, double coneDegrees) {
        double cone = Math.cos(coneDegrees * Mth.DEG_TO_RAD);
        Entity best = null;
        double bestCos = -1.0;
        for (Entity entity : player.level().getEntities(player, new AABB(eye, eye).inflate(range), entity -> isTarget(player, entity))) {
            Vec3 to = entity.getBoundingBox().getCenter().subtract(eye);
            double distance = to.length();
            if (distance > range || distance < 1.0E-3) continue;

            double cos = to.dot(look) / distance;
            if (cos < cone || cos <= bestCos) continue;
            if (!player.hasLineOfSight(entity)) continue;

            best = entity;
            bestCos = cos;
        }
        return best;
    }

    public static List<Entity> inCone(ServerPlayer player, Vec3 eye, Vec3 look, double range, double coneDegrees) {
        double cone = Math.cos(coneDegrees * Mth.DEG_TO_RAD);
        List<Entity> found = new ArrayList<>();
        for (Entity entity : player.level().getEntities(player, new AABB(eye, eye).inflate(range), entity -> isTarget(player, entity))) {
            Vec3 to = entity.getBoundingBox().getCenter().subtract(eye);
            double distance = to.length();
            if (distance > range || distance < 1.0E-3 || to.dot(look) / distance < cone) continue;
            if (player.hasLineOfSight(entity)) found.add(entity);
        }
        found.sort(Comparator.comparingDouble(entity -> -entity.getBoundingBox().getCenter().subtract(eye).normalize().dot(look)));
        return found;
    }

    public static @Nullable Entity nearest(ServerPlayer player, Vec3 from, double range, Set<Entity> exclude) {
        Entity best = null;
        double bestDistance = range * range;
        for (Entity entity : player.level().getEntities(player, new AABB(from, from).inflate(range), entity -> isTarget(player, entity) && !exclude.contains(entity))) {
            double distance = entity.getBoundingBox().getCenter().distanceToSqr(from);
            if (distance > bestDistance) continue;
            best = entity;
            bestDistance = distance;
        }
        return best;
    }

    public static List<Entity> along(ServerPlayer player, Vec3 from, Vec3 to, double padding, Set<Entity> exclude) {
        ServerLevel level = player.level();
        List<Entity> found = new ArrayList<>();
        for (Entity entity : level.getEntities(player, new AABB(from, to).inflate(padding), entity -> isTarget(player, entity) && !exclude.contains(entity))) {
            AABB box = entity.getBoundingBox().inflate(padding);
            if (box.contains(from) || box.clip(from, to).isPresent()) found.add(entity);
        }
        found.sort(Comparator.comparingDouble(entity -> entity.getBoundingBox().getCenter().distanceToSqr(from)));
        return found;
    }

    public static void strike(ServerPlayer player, Entity entity, float damage, Vec3 push) {
        ServerLevel level = player.level();
        if (entity instanceof LivingEntity living) {
            living.setInvulnerableTime(0);
            living.hurtServer(level, RingDamage.source(level, player), damage);
        }
        entity.push(push);
        entity.needsSync = true;
    }

    public static void broadcast(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
        for (ServerPlayer watcher : PlayerLookup.tracking(player)) {
            if (watcher != player) ServerPlayNetworking.send(watcher, payload);
        }
    }

    private ProjectileTargeting() {}
}
