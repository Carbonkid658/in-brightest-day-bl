package dev.amble.core.progression;

import dev.amble.config.BrightestDayConfig;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.impl.FlightRingPower;
import dev.amble.core.team.RingDamage;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.team.LanternTeams;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EmotionSources {
    private static final float LOW_HEALTH = 0.3F;
    private static final float LOW_WIN_HEALTH = 6.0F;
    private static final long LOW_WIN_WINDOW = 600;
    private static final long FEAR_WINDOW = 200;
    private static final long BURST_WINDOW = 1200;
    private static final long PEACE_TICKS = 1200;
    private static final int MAX_FLIGHT_STEP = 200;
    private static final float GUARD_HEALTH = 4.0F;
    private static final long GUARD_TICKS = 200;
    private static final double GUARD_RANGE = 16.0;
    private static final int WEAK_ARMOR = 10;
    private static final Set<EntityType<?>> OUTCLASSING = Set.of(EntityTypes.WARDEN, EntityTypes.WITHER, EntityTypes.ELDER_GUARDIAN,
            EntityTypes.RAVAGER, EntityTypes.EVOKER);

    private record Fear(UUID yellow, long until) {}

    private record Guard(UUID ward, UUID guardian, long until) {}

    private static final Map<String, Long> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> LAST_LOW = new HashMap<>();
    private static final Map<UUID, Fear> FEARED = new HashMap<>();
    private static final Map<UUID, Deque<Long>> KILLS = new HashMap<>();
    private static final Map<UUID, Guard> GUARDS = new HashMap<>();
    private static final Map<UUID, Long> LAST_KILL = new HashMap<>();
    private static final Map<UUID, Vec3> LAST_POSITION = new HashMap<>();

    public static void init() {
        ServerLivingEntityEvents.AFTER_DEATH.register(EmotionSources::onDeath);
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer server) onMined(server, state);
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (player instanceof ServerPlayer server) onUseBlock(server, player.getItemInHand(hand));
            return InteractionResult.PASS;
        });
        ServerTickEvents.END_SERVER_TICK.register(EmotionSources::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUUID();
            LAST_LOW.remove(uuid);
            LAST_KILL.remove(uuid);
            LAST_POSITION.remove(uuid);
            KILLS.remove(uuid);
            COOLDOWNS.keySet().removeIf(key -> key.startsWith(uuid.toString()));
        });
    }

    private static boolean ready(ServerPlayer player, String source, long ticks) {
        String key = player.getUUID() + source;
        long now = player.level().getGameTime();
        Long last = COOLDOWNS.get(key);
        if (last != null && now - last < ticks) return false;
        COOLDOWNS.put(key, now);
        return true;
    }

    private static BrightestDayConfig config() {
        return BrightestDayConfig.get();
    }

    public static void feared(LivingEntity victim, ServerPlayer yellow) {
        FEARED.put(victim.getUUID(), new Fear(yellow.getUUID(), yellow.level().getGameTime() + FEAR_WINDOW));
        RingRanks.fire(yellow, Trigger.FEAR, Milestone.Context.of(victim));
    }

    public static boolean recentlyLow(ServerPlayer player) {
        Long low = LAST_LOW.get(player.getUUID());
        return low != null && player.level().getGameTime() - low <= LOW_WIN_WINDOW;
    }

    public static boolean peaceful(ServerPlayer player) {
        Long kill = LAST_KILL.get(player.getUUID());
        return kill == null || player.level().getGameTime() - kill > PEACE_TICKS;
    }

    public static void onHurt(LivingEntity victim, DamageSource source, float amount) {
        long now = victim.level().getGameTime();
        if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim) {
            if (attacker.getHealth() < attacker.getMaxHealth() * LOW_HEALTH && ready(attacker, "will_low", 20)) {
                SpectrumMeters.add(attacker, Emotion.WILL, config().willLowHit);
            }
            if (victim instanceof Animal && ready(attacker, "fear_flee", 100)) {
                Terror.add(attacker, victim, config().fearFlee);
            }
            if (victim instanceof ServerPlayer hurt && ready(hurt, "rage_hurt", 20)) {
                SpectrumMeters.add(hurt, Emotion.RAGE, config().rageHurtByPlayer);
            }
            if (source.is(RingDamage.RING_CONSTRUCT)) RingRanks.fire(attacker, Trigger.RING_DAMAGE, Milestone.Context.of(victim), Math.round(amount));
            Terror.onHurt(victim, attacker);
        }

        if (!(victim instanceof ServerPlayer ward) || !ward.isAlive()) return;
        if (ward.getHealth() < LOW_WIN_HEALTH) LAST_LOW.put(ward.getUUID(), now);
        if (ward.getHealth() >= GUARD_HEALTH || GUARDS.containsKey(ward.getUUID())) return;
        for (ServerPlayer guardian : ward.level().players()) {
            if (guardian == ward || guardian.distanceToSqr(ward) > GUARD_RANGE * GUARD_RANGE) continue;
            if (PowerRingItem.getWornCorps(guardian).orElse(null) != LanternCorps.STAR_SAPPHIRE || !LanternTeams.areTeammates(guardian, ward)) continue;
            GUARDS.put(ward.getUUID(), new Guard(ward.getUUID(), guardian.getUUID(), now + GUARD_TICKS));
            break;
        }
    }

    private static void onDeath(LivingEntity victim, DamageSource source) {
        if (victim instanceof Player) GUARDS.remove(victim.getUUID());
        Fear fear = FEARED.remove(victim.getUUID());
        long now = victim.level().getGameTime();
        MinecraftServer server = victim.level().getServer();
        boolean feared = fear != null && now <= fear.until();
        if (feared && victim instanceof Player && server != null) {
            ServerPlayer yellow = server.getPlayerList().getPlayer(fear.yellow());
            if (yellow != null) RingRanks.fire(yellow, Trigger.FEARED_DEATH, Milestone.Context.of(victim));
        }

        if (!(source.getEntity() instanceof ServerPlayer killer) || killer == victim) return;
        BrightestDayConfig config = config();
        LAST_KILL.put(killer.getUUID(), now);

        if (victim instanceof Player) {
            SpectrumMeters.add(killer, Emotion.RAGE, config.rageKillPlayer);
        } else if (victim instanceof Animal || victim instanceof NeutralMob) {
            SpectrumMeters.add(killer, Emotion.RAGE, config.rageKillAnimal);
        }

        if (killer.isShiftKeyDown() || killer.isInvisible()) Terror.add(killer, victim, config.fearStealthKill);
        if (victim.hasEffect(MobEffects.SLOWNESS) || victim.hasEffect(MobEffects.WEAKNESS)) Terror.add(killer, victim, config.fearFeebleKill);
        Terror.onKill(victim, killer);
        if (OUTCLASSING.contains(victim.getType())) SpectrumMeters.add(killer, Emotion.WILL, config.willOutclassKill);

        boolean byYellow = feared && fear.yellow().equals(killer.getUUID());
        RingRanks.fire(killer, Trigger.KILL, new Milestone.Context(victim, "", source.is(RingDamage.RING_CONSTRUCT), byYellow));

        if (victim instanceof Mob mob && mob.getTarget() instanceof LivingEntity hunted && hunted != killer) {
            if (hunted instanceof Player) SpectrumMeters.add(killer, Emotion.COMPASSION, config.compassionRescue);
            RingRanks.fire(killer, Trigger.RESCUE, Milestone.Context.of(hunted));
        }

        if (victim instanceof Enemy || victim instanceof Player) {
            Deque<Long> kills = KILLS.computeIfAbsent(killer.getUUID(), uuid -> new ArrayDeque<>());
            kills.addLast(now);
            while (!kills.isEmpty() && now - kills.peekFirst() > BURST_WINDOW) kills.removeFirst();
            RingRanks.fire(killer, Trigger.STREAK, Milestone.Context.NONE, kills.size());
        }
    }

    private static void onMined(ServerPlayer player, BlockState state) {
        BrightestDayConfig config = config();
        if (state.is(Blocks.ANCIENT_DEBRIS)) {
            SpectrumMeters.add(player, Emotion.AVARICE, config.avariceDebris);
            RingRanks.fire(player, Trigger.MINE, Milestone.Context.of("debris"));
            RingOffers.considerOrange(player);
        } else if (state.is(BlockItemTags.DIAMOND_ORES.block())) {
            SpectrumMeters.add(player, Emotion.AVARICE, config.avariceDiamond);
            RingRanks.fire(player, Trigger.MINE, Milestone.Context.of("diamond"));
            RingOffers.considerOrange(player);
        } else if (state.is(BlockItemTags.EMERALD_ORES.block())) {
            SpectrumMeters.add(player, Emotion.AVARICE, config.avariceEmerald);
            RingRanks.fire(player, Trigger.MINE, Milestone.Context.of("emerald"));
        }
    }

    private static void onUseBlock(ServerPlayer player, ItemStack held) {
        boolean planting = held.is(Items.BONE_MEAL) || held.is(ItemTags.SAPLINGS) || held.is(ItemTags.VILLAGER_PLANTABLE_SEEDS);
        if (planting && ready(player, "hope_plant", 20)) {
            SpectrumMeters.add(player, Emotion.HOPE, config().hopePlant);
            RingRanks.fire(player, Trigger.PLANT, Milestone.Context.NONE);
        }
    }

    public static void onTrade(ServerPlayer player) {
        SpectrumMeters.add(player, Emotion.HOPE, config().hopeTrade);
        RingRanks.fire(player, Trigger.TRADE, Milestone.Context.NONE);
    }

    public static void onCure(ServerPlayer player) {
        SpectrumMeters.add(player, Emotion.HOPE, config().hopeCure);
        RingRanks.fire(player, Trigger.CURE, Milestone.Context.NONE);
    }

    public static void onBreed(ServerPlayer player) {
        SpectrumMeters.add(player, Emotion.LOVE, config().loveBreed);
        RingRanks.fire(player, Trigger.BREED, Milestone.Context.NONE);
    }

    public static void onTame(ServerPlayer player) {
        SpectrumMeters.add(player, Emotion.LOVE, config().loveTame);
        RingRanks.fire(player, Trigger.TAME, Milestone.Context.NONE);
    }

    public static void onGift(ServerPlayer giver, Player receiver) {
        if (ready(giver, "love_gift", 40)) {
            SpectrumMeters.add(giver, Emotion.LOVE, config().loveGift);
            RingRanks.fire(giver, Trigger.GIFT, Milestone.Context.of(receiver));
        }
        if (receiver.getHealth() < receiver.getMaxHealth() * LOW_HEALTH && ready(giver, "compassion_gift", 40)) {
            SpectrumMeters.add(giver, Emotion.COMPASSION, config().compassionGift);
        }
    }

    private static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        FEARED.values().removeIf(fear -> now > fear.until());

        Iterator<Guard> guards = GUARDS.values().iterator();
        while (guards.hasNext()) {
            Guard guard = guards.next();
            if (now < guard.until()) continue;
            guards.remove();
            ServerPlayer ward = server.getPlayerList().getPlayer(guard.ward());
            ServerPlayer guardian = server.getPlayerList().getPlayer(guard.guardian());
            if (ward != null && ward.isAlive() && guardian != null) RingRanks.fire(guardian, Trigger.GUARD, Milestone.Context.of(ward));
        }

        if (server.getTickCount() % 20 != 0) return;
        boolean minute = server.getTickCount() % 1200 == 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator()) continue;
            RingRanks.assign(player);
            RingRanks.fire(player, Trigger.SECOND, Milestone.Context.NONE);

            Vec3 previous = LAST_POSITION.put(player.getUUID(), player.position());
            if (previous != null && FlightRingPower.isFlying(player)) {
                int distance = (int) Math.round(previous.distanceTo(player.position()));
                if (distance > 0 && distance < MAX_FLIGHT_STEP) RingRanks.fire(player, Trigger.FLY, Milestone.Context.NONE, distance);
            }

            if (minute && player.level().dimension() != Level.OVERWORLD && player.getArmorValue() < WEAK_ARMOR) {
                SpectrumMeters.add(player, Emotion.WILL, config().willHostileMinute);
            }
        }
    }

    private EmotionSources() {}
}
