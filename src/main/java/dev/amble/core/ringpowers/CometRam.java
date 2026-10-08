package dev.amble.core.ringpowers;

import dev.amble.core.ringpowers.impl.FlightRingPower;
import dev.amble.core.team.LanternTeams;
import dev.amble.core.team.RingDamage;
import dev.amble.core.team.RingTargets;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class CometRam {
    private static final double MIN_SPEED = 1.2;
    private static final float BASE_DAMAGE = 6.0F;
    private static final float DAMAGE_PER_SPEED = 3.0F;
    private static final double KNOCKBACK = 1.4;
    private static final double LIFT = 0.4;
    private static final double REACH = 0.6;
    private static final int IGNITE_SECONDS = 3;
    private static final long RAM_COOLDOWN = 20;

    private record Hit(UUID ram, UUID target) {}

    private static final Map<ServerPlayer, Vec3> LAST_POSITION = new WeakHashMap<>();
    private static final Map<Hit, Long> RAMMED = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(CometRam::tick);
    }

    private static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (server.getTickCount() % 100 == 0) RAMMED.values().removeIf(time -> now - time > RAM_COOLDOWN);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Vec3 last = LAST_POSITION.put(player, player.position());
            if (last == null || player.isSpectator() || !RedRage.isRed(player)
                    || !FlightRingPower.isFlying(player) || !FlightRingPower.isBoosting(player)) continue;

            Vec3 velocity = player.position().subtract(last);
            double speed = velocity.length();
            if (speed < MIN_SPEED || speed > 20.0) continue;
            ram(player.level(), player, velocity, speed, now);
        }
    }

    private static void ram(ServerLevel level, ServerPlayer player, Vec3 velocity, double speed, long now) {
        Vec3 behind = player.position().subtract(velocity.normalize().scale(0.8));
        level.sendParticles(ParticleTypes.FLAME, behind.x, behind.y + 0.9, behind.z, 6, 0.3, 0.3, 0.3, 0.01);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, behind.x, behind.y + 0.9, behind.z, 1, 0.2, 0.2, 0.2, 0.0);

        AABB swept = player.getBoundingBox().expandTowards(velocity.scale(-1.0)).inflate(REACH);
        Vec3 push = velocity.normalize();
        float damage = BASE_DAMAGE + DAMAGE_PER_SPEED * (float) speed;
        for (Entity target : level.getEntities(player, swept,
                entity -> RingTargets.root(entity) != player && RingTargets.isHittable(entity) && !LanternTeams.areTeammates(player, RingTargets.root(entity)))) {
            Hit hit = new Hit(player.getUUID(), RingTargets.root(target).getUUID());
            Long previous = RAMMED.get(hit);
            if (previous != null && now - previous < RAM_COOLDOWN) continue;
            RAMMED.put(hit, now);

            if (!target.hurtServer(level, RingDamage.source(level, player), damage)) continue;
            target.push(push.x * KNOCKBACK, LIFT, push.z * KNOCKBACK);
            target.needsSync = true;
            target.igniteForSeconds(IGNITE_SECONDS);
            level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.7F);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.2F, 0.8F);
        }
    }

    private CometRam() {}
}
