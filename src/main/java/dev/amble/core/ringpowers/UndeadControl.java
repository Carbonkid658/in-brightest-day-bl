package dev.amble.core.ringpowers;

import dev.amble.core.items.PowerRingItem;
import dev.amble.BrightestDay;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Black Lantern's command over the dead. Undead mobs near the bearer are bound to them and behave like
 * tamed dogs: they follow, never turn on their master, attack whatever their master hurts, and defend
 * their master when it is hurt.
 */
public final class UndeadControl {
    public static final double RADIUS = 7.0;
    public static final int MAX_CONTROLLED = 10;
    private static final int UPDATE_INTERVAL = 5;
    private static final int PARTICLE_INTERVAL = 20;
    private static final int TARGET_MEMORY = 200;
    private static final double FOLLOW_DISTANCE = 6.0;
    /** A bound mob that strays this far from its master slips the leash and is simply freed. */
    private static final double UNLINK_DISTANCE = 24.0;
    private static final double FOLLOW_SPEED = 1.2;
    // The black corps' own colour is nearly invisible on the action bar; messages use this lighter grey.
    public static final int TEXT_COLOR = LanternCorps.BLACK.textColor();

    /** Set on every mob currently bound to a Black Lantern; synced so clients can tint it black. Never saved. */
    public static final AttachmentType<Boolean> BOUND =
            AttachmentRegistry.<Boolean>builder()
                    .syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all())
                    .buildAndRegister(BrightestDay.id("bound_dead"));

    private static final Map<UUID, Control> CONTROLS = new HashMap<>();

    private static final class Control {
        final Set<UUID> mobs = new LinkedHashSet<>();
        @Nullable UUID target;
        long targetUntil;
        long lastHurtMob;
        long lastHurtBy;
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(UndeadControl::tick);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) release(player.level().getServer(), player.getUUID(), false);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> release(server, handler.player.getUUID(), false));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CONTROLS.clear());
    }

    public static boolean isUndead(Entity entity) {
        return entity instanceof Mob && entity.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD);
    }

    public static boolean isControlled(Entity entity) {
        for (Control control : CONTROLS.values()) {
            if (control.mobs.contains(entity.getUUID())) return true;
        }
        return false;
    }

    public static int controlledCount(ServerPlayer player) {
        Control control = CONTROLS.get(player.getUUID());
        return control == null ? 0 : control.mobs.size();
    }

    /**
     * Binds every free undead mob within {@link #RADIUS} blocks of the player.
     * Sneaking releases everything instead. Returns the number newly bound, or -1 when the dead were released.
     */
    public static int command(ServerPlayer player, int color) {
        ServerLevel level = player.level();
        if (player.isShiftKeyDown()) {
            if (controlledCount(player) == 0) return 0;
            release(level.getServer(), player.getUUID(), true);
            return -1;
        }

        Control control = CONTROLS.computeIfAbsent(player.getUUID(), id -> new Control());
        List<Mob> nearby = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RADIUS),
                mob -> mob.isAlive() && isUndead(mob) && mob.distanceTo(player) <= RADIUS && !isControlled(mob));
        int bound = 0;
        for (Mob mob : nearby) {
            if (control.mobs.size() >= MAX_CONTROLLED) break;
            control.mobs.add(mob.getUUID());
            mob.setAttached(BOUND, true);
            mob.setTarget(null);
            mob.setPersistenceRequired();
            level.sendParticles(new DustParticleOptions(color, 1.6F), mob.getX(), mob.getY(0.5), mob.getZ(), 14, 0.3, 0.5, 0.3, 0.0);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, mob.getX(), mob.getY(0.5), mob.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
            bound++;
        }
        if (control.mobs.isEmpty()) CONTROLS.remove(player.getUUID());
        if (bound > 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.5F, 0.6F);
        }
        return bound;
    }

    /** Releases every mob this player controls. Returns false if they controlled none. */
    public static boolean releaseAll(ServerPlayer player) {
        if (controlledCount(player) == 0) return false;
        release(player.level().getServer(), player.getUUID(), true);
        return true;
    }

    private static void release(MinecraftServer server, UUID ownerId, boolean notify) {
        Control control = CONTROLS.remove(ownerId);
        if (control == null) return;
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        for (UUID id : control.mobs) {
            Entity entity = find(server, id);
            if (entity instanceof Mob mob) unbind(mob);
        }
        if (notify && owner != null) {
            owner.sendOverlayMessage(Component.translatable("message.brightestday.undead.released").withColor(TEXT_COLOR));
        }
    }

    private static void unbind(Mob mob) {
        mob.setTarget(null);
        mob.removeAttached(BOUND);
    }

    private static @Nullable Entity find(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity != null) return entity;
        }
        return null;
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % UPDATE_INTERVAL != 0 || CONTROLS.isEmpty()) return;
        boolean pulse = server.getTickCount() % PARTICLE_INTERVAL == 0;

        Iterator<Map.Entry<UUID, Control>> controls = CONTROLS.entrySet().iterator();
        while (controls.hasNext()) {
            Map.Entry<UUID, Control> entry = controls.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            Control control = entry.getValue();
            if (owner == null || !owner.isAlive() || PowerRingItem.getWornCorps(owner).orElse(null) != LanternCorps.BLACK) {
                for (UUID id : control.mobs) {
                    if (find(server, id) instanceof Mob mob) unbind(mob);
                }
                controls.remove();
                continue;
            }
            update(server, owner, control, pulse);
            if (control.mobs.isEmpty()) controls.remove();
        }
    }

    private static void update(MinecraftServer server, ServerPlayer owner, Control control, boolean pulse) {
        ServerLevel level = owner.level();
        long now = level.getGameTime();

        // Like a wolf: strike what the master strikes, and answer whatever strikes the master.
        LivingEntity struck = owner.getLastHurtMob();
        long struckAt = owner.getLastHurtMobTimestamp();
        if (struck != null && struckAt != control.lastHurtMob && validTarget(owner, struck)) mark(control, struck, now);
        control.lastHurtMob = struckAt;

        LivingEntity attacker = owner.getLastHurtByMob();
        long attackedAt = owner.getLastHurtByMobTimestamp();
        if (attacker != null && attackedAt != control.lastHurtBy && validTarget(owner, attacker)) mark(control, attacker, now);
        control.lastHurtBy = attackedAt;

        LivingEntity target = null;
        if (control.target != null && now <= control.targetUntil && level.getEntity(control.target) instanceof LivingEntity living
                && living.isAlive() && validTarget(owner, living)) {
            target = living;
        } else {
            control.target = null;
        }

        Iterator<UUID> mobs = control.mobs.iterator();
        while (mobs.hasNext()) {
            UUID id = mobs.next();
            if (!(level.getEntity(id) instanceof Mob mob) || !mob.isAlive()) {
                mobs.remove();
                continue;
            }
            if (mob.distanceTo(owner) > UNLINK_DISTANCE) {
                unbind(mob);
                mob.getNavigation().stop();
                mobs.remove();
                continue;
            }
            if (target != null) {
                if (mob.getTarget() != target) mob.setTarget(target);
            } else {
                if (mob.getTarget() != null) mob.setTarget(null);
                if (mob.distanceTo(owner) > FOLLOW_DISTANCE) mob.getNavigation().moveTo(owner, FOLLOW_SPEED);
            }
            if (pulse) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, mob.getX(), mob.getY(1.0), mob.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
            }
        }
    }

    private static void mark(Control control, LivingEntity target, long now) {
        control.target = target.getUUID();
        control.targetUntil = now + TARGET_MEMORY;
    }

    private static boolean validTarget(ServerPlayer owner, LivingEntity target) {
        return target != owner && target.isAlive() && !target.isSpectator() && !isControlled(target) && !owner.isAlliedTo(target);
    }

    private UndeadControl() {}
}
