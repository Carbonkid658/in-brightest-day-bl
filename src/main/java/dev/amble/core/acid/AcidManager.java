package dev.amble.core.acid;

import dev.amble.config.BrightestDayConfig;
import dev.amble.core.team.RingDamage;
import dev.amble.core.team.RingTargets;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.networking.payloads.s2c.AcidS2CPayload;
import dev.amble.core.ringpowers.RingPowerRegistry;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AcidManager {
    private static final int HIT_INTERVAL = 10;
    private static final int SIZZLE_INTERVAL = 6;
    private static final int MAX_STEPS = 30;
    private static final double HIT_PADDING = 0.4;

    private static final Map<ServerPlayer, Integer> SPEWING = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(AcidManager::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SPEWING.remove(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SPEWING.clear());
    }

    public static boolean isSpewing(ServerPlayer player) {
        return SPEWING.containsKey(player);
    }

    public static void start(ServerPlayer player) {
        if (SPEWING.containsKey(player) || player.isSpectator() || BrightestDayAttachments.get(player, RingPowerRegistry.ACID).isEmpty()) return;
        if (!PowerRingItem.hasCharge(player)) {
            player.sendOverlayMessage(Component.translatable("message.brightestday.ring_depleted"));
            return;
        }

        SPEWING.put(player, 0);
        ArmedRingPower.raise(player);
        broadcast(player, true);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.2F, 0.5F);
    }

    public static void stop(ServerPlayer player) {
        if (SPEWING.remove(player) == null) return;
        broadcast(player, false);
    }

    private static void tick(MinecraftServer server) {
        BrightestDayConfig config = BrightestDayConfig.get();
        for (ServerPlayer player : List.copyOf(SPEWING.keySet())) {
            int age = SPEWING.merge(player, 1, Integer::sum);
            boolean drained = age % 20 == 0 && !player.hasInfiniteMaterials() && !PowerRingItem.drainWorn(player, config.acidDrainPerSecond);
            if (player.isRemoved() || !player.isAlive() || player.isSpectator() || !PowerRingItem.hasCharge(player) || drained
                    || BrightestDayAttachments.get(player, RingPowerRegistry.ACID).isEmpty()) {
                stop(player);
                continue;
            }

            List<Vec3> path = path(player, config.acidRange);
            if (age % HIT_INTERVAL == 0) burn(player, path, config);
            if (age % SIZZLE_INTERVAL == 0) {
                Vec3 end = path.getLast();
                player.level().playSound(null, end.x, end.y, end.z, SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.4F + player.getRandom().nextFloat() * 0.4F);
            }
        }
    }

    private static List<Vec3> path(ServerPlayer player, double range) {
        ServerLevel level = player.level();
        Vec3 origin = AcidStream.mouth(player, 1.0F);
        Vec3 velocity = AcidStream.velocity(player.getLookAngle());
        Vec3 position = origin;
        List<Vec3> path = new ArrayList<>();
        path.add(position);

        for (int step = 0; step < MAX_STEPS; step++) {
            Vec3 next = position.add(velocity);
            BlockHitResult hit = level.clip(new ClipContext(position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                path.add(hit.getLocation());
                break;
            }
            path.add(next);
            if (next.distanceTo(origin) > range) break;
            position = next;
            velocity = velocity.add(0.0, -AcidStream.GRAVITY, 0.0);
        }
        return path;
    }

    private static void burn(ServerPlayer player, List<Vec3> path, BrightestDayConfig config) {
        ServerLevel level = player.level();
        Set<Entity> hit = new HashSet<>();
        for (int i = 1; i < path.size(); i++) {
            Vec3 from = path.get(i - 1);
            Vec3 to = path.get(i);
            for (Entity entity : level.getEntities(player, new AABB(from, to).inflate(HIT_PADDING), entity -> RingTargets.isTarget(player, entity))) {
                AABB box = entity.getBoundingBox().inflate(HIT_PADDING);
                if (RingTargets.excluded(hit, entity)) continue;
                if (box.contains(from) || box.clip(from, to).isPresent()) hit.add(entity);
            }
        }

        for (Entity entity : hit) {
            if (!(entity instanceof LivingEntity living)) {
                RingTargets.hurt(level, entity, RingDamage.source(level, player), config.acidDamage);
                continue;
            }
            living.igniteForSeconds(config.acidFireSeconds);
            living.hurtServer(level, RingDamage.source(level, player), config.acidDamage);
            if (config.acidArmorWear <= 0) continue;
            for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
                ItemStack armor = living.getItemBySlot(slot);
                if (!armor.isEmpty() && armor.isDamageableItem()) armor.hurtAndBreak(config.acidArmorWear, living, slot);
            }
        }
    }

    private static void broadcast(ServerPlayer player, boolean active) {
        AcidS2CPayload payload = new AcidS2CPayload(player.getId(), active);
        ServerPlayNetworking.send(player, payload);
        for (ServerPlayer watcher : PlayerLookup.tracking(player)) {
            if (watcher != player) ServerPlayNetworking.send(watcher, payload);
        }
    }

    private AcidManager() {}
}
