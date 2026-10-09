package dev.amble.core.ringpowers;

import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.BrightestDayComponents;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.progression.Emotion;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Black Lanterns have no lantern to recharge at. They feed on emotion instead: the ring slowly fills while
 * the bearer is near other lanterns, or near creatures that are feeling something.
 *
 * <ul>
 *   <li>Rage: being attacked by a mob</li>
 *   <li>Greed: villagers nearby</li>
 *   <li>Fear: a mob nearby that is panicking</li>
 *   <li>Will: an iron golem nearby that is fighting</li>
 *   <li>Hope: allays nearby</li>
 *   <li>Compassion: a zombie villager nearby that is being cured</li>
 *   <li>Love: animals breeding nearby</li>
 *   <li>Other lanterns: players nearby bearing a different corps' ring</li>
 * </ul>
 * Every amount is a fraction of {@link BrightestDayComponents#MAX_POWER}; tune them below.
 */
public final class BlackFeeding {
    private static final int UPDATE_INTERVAL = 5;
    private static final int AMBIENT_INTERVAL = 20;

    private static final double MOB_RADIUS = 12.0;
    private static final double LANTERN_RADIUS = 16.0;
    private static final double BREED_RADIUS = 16.0;

    // Charge gained (out of MAX_POWER = 5000). Per-second amounts are per creature, up to the cap.
    // Killing a mob outright (it was at full health and died to one blow) feeds the ring this fraction of its capacity.
    private static final float ONE_SHOT_FRACTION = 0.01F;
    private static final long ONE_SHOT_MEMORY = 100L;
    private static final int RAGE_PER_HIT = 60;
    private static final int GREED_PER_VILLAGER = 6;
    private static final int GREED_CAP = 4;
    private static final int FEAR_PER_MOB = 10;
    private static final int FEAR_CAP = 3;
    private static final int WILL_PER_GOLEM = 8;
    private static final int WILL_CAP = 2;
    // A mob that has set its sights on another mob (controlled or not) feeds the bearer's Will.
    private static final int AGGRO_PER_MOB = 6;
    private static final int AGGRO_CAP = 5;
    private static final int HOPE_PER_ALLAY = 6;
    private static final int HOPE_CAP = 3;
    private static final int COMPASSION_PER_CURE = 12;
    private static final int COMPASSION_CAP = 2;
    private static final int LOVE_PER_BREEDING = 150;
    private static final int LANTERN_PER_BEARER = 8;
    private static final int LANTERN_CAP = 3;
    private static final int LANTERN_COLOR = 0xF4F4F4;

    private static final Map<UUID, Long> LAST_HURT_BY = new HashMap<>();
    /** Mobs a Black Lantern struck while they were at full health, and when. */
    private static final Map<UUID, Long> FULL_HEALTH_HITS = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(BlackFeeding::tick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof Mob && source.getEntity() instanceof ServerPlayer attacker && isBlack(attacker)
                    && entity.getHealth() >= entity.getMaxHealth()) {
                FULL_HEALTH_HITS.put(entity.getUUID(), entity.level().getGameTime());
            }
            return true;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            Long struck = FULL_HEALTH_HITS.remove(entity.getUUID());
            if (struck == null || !(entity instanceof Mob) || struck != entity.level().getGameTime()) return;
            if (!(source.getEntity() instanceof ServerPlayer killer) || !isBlack(killer)) return;
            Map<Integer, Integer> gains = new LinkedHashMap<>();
            gain(gains, Emotion.RAGE.corps().color(), Math.round(BrightestDayComponents.MAX_POWER * ONE_SHOT_FRACTION));
            apply(killer, gains);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_HURT_BY.remove(handler.player.getUUID()));
    }

    public static boolean isBlack(Player player) {
        return PowerRingItem.getWornCorps(player).orElse(null) == LanternCorps.BLACK;
    }

    /** The dead recognise their own: only these undead leave a Black Lantern alone (unless the bearer has hurt them). */
    private static final Set<String> KIN = Set.of(
            "zombie", "zombie_horse", "zombie_villager", "husk", "drowned", "camel_husk", "skeleton", "stray",
            "bogged", "parched", "skeleton_horse", "zombified_piglin", "zoglin");

    public static boolean spares(Mob mob, Player player) {
        if (!isBlack(player) || mob.getLastHurtByMob() == player) return false;
        return KIN.contains(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath());
    }

    /** Called when two animals breed: every Black Lantern nearby feeds on the love. */
    public static void onBreed(ServerLevel level, Animal parent) {
        for (ServerPlayer player : level.players()) {
            if (!isBlack(player) || player.distanceTo(parent) > BREED_RADIUS) continue;
            Map<Integer, Integer> gains = new LinkedHashMap<>();
            gains.put(Emotion.LOVE.corps().color(), LOVE_PER_BREEDING);
            apply(player, gains);
        }
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % UPDATE_INTERVAL != 0) return;
        if (!FULL_HEALTH_HITS.isEmpty()) {
            long now = server.overworld().getGameTime();
            FULL_HEALTH_HITS.values().removeIf(at -> now - at > ONE_SHOT_MEMORY);
        }
        boolean ambient = server.getTickCount() % AMBIENT_INTERVAL == 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator() || !isBlack(player)) continue;
            Map<Integer, Integer> gains = new LinkedHashMap<>();

            long hurtAt = player.getLastHurtByMobTimestamp();
            Long previous = LAST_HURT_BY.put(player.getUUID(), hurtAt);
            if (previous != null && previous != hurtAt && player.getLastHurtByMob() instanceof Mob) {
                gain(gains, Emotion.RAGE.corps().color(), RAGE_PER_HIT);
            }
            if (ambient) ambient(player, gains);
            apply(player, gains);
        }
    }

    private static void ambient(ServerPlayer player, Map<Integer, Integer> gains) {
        ServerLevel level = player.level();
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(LANTERN_RADIUS),
                entity -> entity != player && entity.isAlive() && !entity.isSpectator());

        int greed = 0, fear = 0, will = 0, aggro = 0, hope = 0, compassion = 0, lanterns = 0;
        for (LivingEntity entity : nearby) {
            double distance = entity.distanceTo(player);
            if (entity instanceof Player other) {
                LanternCorps corps = PowerRingItem.getWornCorps(other).orElse(null);
                if (corps != null && corps != LanternCorps.BLACK && distance <= LANTERN_RADIUS) lanterns++;
                continue;
            }
            if (distance > MOB_RADIUS) continue;

            String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
            if (entity instanceof Mob attacker && attacker.getTarget() instanceof Mob) aggro++;
            if (type.equals("iron_golem") && entity instanceof Mob guardian && guardian.getTarget() == null && !player.isCreative()) {
                guardian.setTarget(player);
            }
            if (type.equals("villager") || type.equals("pillager") || type.equals("evoker") || type.equals("vindicator")) greed++;
            else if (type.equals("allay")) hope++;
            else if (type.equals("iron_golem") && entity instanceof Mob golem && golem.getTarget() != null) will++;
            else if (entity instanceof ZombieVillager zombie && zombie.isConverting()) compassion++;
            if (entity instanceof PathfinderMob mob && mob.isPanicking()) fear++;
        }

        gain(gains, Emotion.AVARICE.corps().color(), Math.min(greed, GREED_CAP) * GREED_PER_VILLAGER);
        gain(gains, Emotion.FEAR.corps().color(), Math.min(fear, FEAR_CAP) * FEAR_PER_MOB);
        gain(gains, Emotion.WILL.corps().color(), Math.min(will, WILL_CAP) * WILL_PER_GOLEM + Math.min(aggro, AGGRO_CAP) * AGGRO_PER_MOB);
        gain(gains, Emotion.HOPE.corps().color(), Math.min(hope, HOPE_CAP) * HOPE_PER_ALLAY);
        gain(gains, Emotion.COMPASSION.corps().color(), Math.min(compassion, COMPASSION_CAP) * COMPASSION_PER_CURE);
        gain(gains, LANTERN_COLOR, Math.min(lanterns, LANTERN_CAP) * LANTERN_PER_BEARER);
    }

    private static void gain(Map<Integer, Integer> gains, int color, int amount) {
        if (amount > 0) gains.merge(color, amount, Integer::sum);
    }

    private static void apply(ServerPlayer player, Map<Integer, Integer> gains) {
        if (gains.isEmpty()) return;
        ItemStack ring = PowerRingItem.getWornRing(player);
        if (ring.isEmpty() || PowerRingItem.getRingPower(ring) >= BrightestDayComponents.MAX_POWER) return;

        int total = 0;
        for (int amount : gains.values()) total += amount;
        PowerRingItem.chargeRing(ring, total);
        if (ring == BrightestDayAttachments.getRing(player)) BrightestDayAttachments.setRing(player, ring);

        ServerLevel level = player.level();
        for (int color : gains.keySet()) {
            level.sendParticles(new DustParticleOptions(color, 1.0F), player.getX(), player.getY(1.0), player.getZ(), 4, 0.4, 0.6, 0.4, 0.0);
        }
    }

    private BlackFeeding() {}
}
