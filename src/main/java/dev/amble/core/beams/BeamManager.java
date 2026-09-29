package dev.amble.core.beams;

import dev.amble.core.items.PowerRingItem;
import dev.amble.core.networking.payloads.s2c.BeamS2CPayload;
import dev.amble.core.ringpowers.RingPowerRegistry;
import dev.amble.core.ringpowers.constructs.ConstructRingPower;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class BeamManager {
    public static final int MAX_TICKS = 140;
    public static final double RANGE = 32.0;
    private static final int DAMAGE_INTERVAL = 10;
    private static final float DAMAGE = 4.0F;
    private static final double PUSH = 0.08;
    private static final int DRAIN_PER_SECOND = 20;

    private static final Map<ServerPlayer, Beam> BEAMS = new HashMap<>();

    private static final class Beam {
        final int color;
        int age;

        Beam(int color) {
            this.color = color;
        }
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(BeamManager::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> stop(handler.player));
    }

    public static void start(ServerPlayer player, int color) {
        if (BEAMS.containsKey(player)) return;
        BEAMS.put(player, new Beam(color));
        broadcast(player, color, true);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.8F);
    }

    public static void stop(ServerPlayer player) {
        Beam beam = BEAMS.remove(player);
        if (beam == null) return;
        broadcast(player, beam.color, false);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8F, 1.8F);
    }

    private static void tick(MinecraftServer server) {
        boolean drainTick = server.getTickCount() % 20 == 0;

        for (ServerPlayer player : new ArrayList<>(BEAMS.keySet())) {
            Beam beam = BEAMS.get(player);
            boolean selected = ArmedRingPower.selectedConstruct(player).orElse(null) == RingPowerRegistry.BEAM;
            boolean outOfCharge = !PowerRingItem.hasCharge(player)
                    || drainTick && !player.hasInfiniteMaterials() && !PowerRingItem.drainWorn(player, DRAIN_PER_SECOND);
            if (++beam.age > MAX_TICKS || !player.isAlive() || !ArmedRingPower.isArmed(player) || !selected || outOfCharge) {
                stop(player);
                continue;
            }

            ConstructRingPower.Aim aim = ConstructRingPower.aim(player, RANGE);
            if (!(aim.entity() instanceof LivingEntity target)) continue;

            ServerLevel level = player.level();
            if (beam.age % DAMAGE_INTERVAL == 0) target.hurtServer(level, level.damageSources().playerAttack(player), DAMAGE);
            Vec3 push = aim.look().scale(PUSH);
            target.push(push);
            if (beam.age % 4 == 0) target.needsSync = true;
        }
    }

    private static void broadcast(ServerPlayer player, int color, boolean active) {
        BeamS2CPayload payload = new BeamS2CPayload(player.getId(), color, active);
        ServerPlayNetworking.send(player, payload);
        for (ServerPlayer watcher : PlayerLookup.tracking(player)) {
            if (watcher != player) ServerPlayNetworking.send(watcher, payload);
        }
    }

    public static boolean isBeaming(Entity entity) {
        return entity instanceof ServerPlayer player && BEAMS.containsKey(player);
    }

    private BeamManager() {}
}
