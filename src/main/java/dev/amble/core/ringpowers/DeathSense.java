package dev.amble.core.ringpowers;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Death Sense: everything within {@link #RADIUS} blocks glows for {@link #SECONDS} seconds and
 * suffers one tick of withering (1 damage) every second of it.
 */
public final class DeathSense {
    public static final double RADIUS = 4.0;
    public static final int SECONDS = 3;
    private static final float WITHER_TICK_DAMAGE = 1.0F;

    private static final class Mark {
        int pulsesLeft;

        Mark(int pulsesLeft) {
            this.pulsesLeft = pulsesLeft;
        }
    }

    private static final Map<UUID, Mark> MARKS = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(DeathSense::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> MARKS.clear());
    }

    /** Marks everything near the bearer. Returns how many things were marked. */
    public static int pulse(ServerPlayer player) {
        ServerLevel level = player.level();
        int marked = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS),
                candidate -> valid(player, candidate) && candidate.distanceTo(player) <= RADIUS)) {
            MARKS.put(target.getUUID(), new Mark(SECONDS - 1));
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, SECONDS * 20, 0, false, false));
            wither(level, target);
            marked++;
        }
        if (marked > 0) level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.5F, 0.8F);
        return marked;
    }

    private static boolean valid(ServerPlayer owner, LivingEntity target) {
        if (target == owner || !target.isAlive() || target.isSpectator()) return false;
        if (target instanceof ServerPlayer other && (other.hasInfiniteMaterials() || owner.isAlliedTo(other))) return false;
        if (target instanceof TamableAnimal pet && pet.isOwnedBy(owner)) return false;
        return !UndeadControl.isControlled(target);
    }

    private static void wither(ServerLevel level, LivingEntity target) {
        target.hurtServer(level, level.damageSources().wither(), WITHER_TICK_DAMAGE);
        level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY(0.6), target.getZ(), 4, 0.25, 0.4, 0.25, 0.02);
    }

    private static void tick(MinecraftServer server) {
        if (MARKS.isEmpty() || server.getTickCount() % 20 != 0) return;
        Iterator<Map.Entry<UUID, Mark>> iterator = MARKS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Mark> entry = iterator.next();
            Mark mark = entry.getValue();
            LivingEntity target = null;
            ServerLevel where = null;
            for (ServerLevel level : server.getAllLevels()) {
                if (level.getEntity(entry.getKey()) instanceof LivingEntity living) {
                    target = living;
                    where = level;
                    break;
                }
            }
            if (target == null || !target.isAlive() || mark.pulsesLeft <= 0) {
                iterator.remove();
                continue;
            }
            wither(where, target);
            if (--mark.pulsesLeft <= 0) iterator.remove();
        }
    }

    private DeathSense() {}
}
