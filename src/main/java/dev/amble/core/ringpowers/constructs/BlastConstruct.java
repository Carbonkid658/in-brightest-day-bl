package dev.amble.core.ringpowers.constructs;

import dev.amble.BrightestDay;
import dev.amble.core.networking.payloads.s2c.BlastS2CPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BlastConstruct extends ConstructRingPower {
    private static final double RANGE = 48.0;
    private static final double BLAST_RADIUS = 3.5;
    private static final double KNOCKBACK = 1.8;
    private static final double LIFT = 0.35;
    private static final float SPLASH_DAMAGE = 4.0F;
    private static final float DIRECT_DAMAGE = 6.0F;
    private static final int USE_COST = 250;

    public BlastConstruct() {
        super(BrightestDay.id("blast"));
    }

    @Override
    public int useCost() {
        return USE_COST;
    }

    @Override
    public void fire(ServerPlayer player, int radius, int color) {
        ServerLevel level = player.level();
        Aim aim = aim(player, RANGE);
        Vec3 impact = aim.end();
        Entity direct = aim.entity();
        DamageSource source = level.damageSources().playerAttack(player);

        for (Entity entity : level.getEntities(player, new AABB(impact, impact).inflate(BLAST_RADIUS))) {
            Vec3 center = entity.getBoundingBox().getCenter();
            double distance = center.distanceTo(impact);
            if (distance > BLAST_RADIUS && entity != direct) continue;

            double falloff = entity == direct ? 1.0 : 1.0 - distance / BLAST_RADIUS;
            if (entity instanceof LivingEntity living) {
                float damage = (float) (SPLASH_DAMAGE * falloff) + (entity == direct ? DIRECT_DAMAGE : 0.0F);
                living.hurtServer(level, source, damage);
            }

            Vec3 away = center.subtract(impact);
            away = away.lengthSqr() < 1.0E-4 ? aim.look() : away.normalize();
            Vec3 push = away.add(aim.look()).normalize().scale(KNOCKBACK * falloff).add(0.0, LIFT * falloff, 0.0);
            entity.push(push);
            entity.needsSync = true;
        }

        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.4F);
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1.2F, 0.8F);

        BlastS2CPayload payload = new BlastS2CPayload(player.getId(), impact, color);
        ServerPlayNetworking.send(player, payload);
        for (ServerPlayer watcher : PlayerLookup.tracking(player)) {
            if (watcher != player) ServerPlayNetworking.send(watcher, payload);
        }
    }
}
