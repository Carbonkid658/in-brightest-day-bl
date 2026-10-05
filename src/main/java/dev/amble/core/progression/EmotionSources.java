package dev.amble.core.progression;

import dev.amble.config.BrightestDayConfig;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.CorpsCombat;
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
    private static final int BURST_KILLS = 10;
    private static final int HOARD_DIAMONDS = 32;
    private static final float GUARD_HEALTH = 4.0F;
    private static final long GUARD_TICKS = 200;
    private static final double GUARD_RANGE = 16.0;
    private static final int WEAK_ARMOR = 10;
    private static final Set<EntityType<?>> OUTCLASSING = Set.of(EntityTypes.WARDEN, EntityTypes.WITHER, EntityTypes.ELDER_GUARDIAN,
            EntityTypes.RAVAGER, EntityTypes.EVOKER);
    private static final Set<EntityType<?>> BOSSES = Set.of(EntityTypes.WARDEN, EntityTypes.WITHER);

    private record Fear(UUID yellow, long until) {}

    private record Guard(UUID ward, UUID guardian, long until) {}

    private static final Map<String, Long> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> LAST_LOW = new HashMap<>();
    private static final Map<UUID, Fear> FEARED = new HashMap<>();
    private static final Map<UUID, Deque<Long>> KILLS = new HashMap<>();
    private static final Map<UUID, Guard> GUARDS = new HashMap<>();

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
        RingRanks.progress(yellow, RankTask.YELLOW_FEAR, 1);
    }

    public static void onHurt(LivingEntity victim, DamageSource source, float amount) {
        long now = victim.level().getGameTime();
        if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim) {
            if (attacker.getHealth() < attacker.getMaxHealth() * LOW_HEALTH && ready(attacker, "will_low", 20)) {
                SpectrumMeters.add(attacker, Emotion.WILL, config().willLowHit);
            }
            if (victim instanceof Animal && ready(attacker, "fear_flee", 100)) {
                SpectrumMeters.add(attacker, Emotion.FEAR, config().fearFlee);
            }
            if (victim instanceof ServerPlayer hurt && ready(hurt, "rage_hurt", 20)) {
                SpectrumMeters.add(hurt, Emotion.RAGE, config().rageHurtByPlayer);
            }
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
        if (fear != null && now <= fear.until() && victim instanceof Player && server != null) {
            ServerPlayer yellow = server.getPlayerList().getPlayer(fear.yellow());
            if (yellow != null) RingRanks.progress(yellow, RankTask.YELLOW_FEARED_DEATHS, 1);
        }

        if (!(source.getEntity() instanceof ServerPlayer killer) || killer == victim) return;
        BrightestDayConfig config = config();

        if (victim instanceof Player) {
            SpectrumMeters.add(killer, Emotion.RAGE, config.rageKillPlayer);
            if (!BrightestDayAttachments.getRing((Player) victim).isEmpty()) {
                RingRanks.progress(killer, RankTask.RED_DUELS, 1);
                RingRanks.progress(killer, RankTask.ORANGE_RING_KILL, 1);
            }
        } else if (victim instanceof Animal || victim instanceof NeutralMob) {
            SpectrumMeters.add(killer, Emotion.RAGE, config.rageKillAnimal);
        }

        if (killer.isShiftKeyDown() || killer.isInvisible()) SpectrumMeters.add(killer, Emotion.FEAR, config.fearStealthKill);
        if (victim.hasEffect(MobEffects.SLOWNESS) || victim.hasEffect(MobEffects.WEAKNESS)) SpectrumMeters.add(killer, Emotion.FEAR, config.fearFeebleKill);
        if (fear != null && now <= fear.until() && fear.yellow().equals(killer.getUUID())) RingRanks.progress(killer, RankTask.YELLOW_FEARED_KILLS, 1);

        if (OUTCLASSING.contains(victim.getType())) {
            SpectrumMeters.add(killer, Emotion.WILL, config.willOutclassKill);
            RingRanks.progress(killer, RankTask.GREEN_OUTCLASS, 1);
        }
        if (BOSSES.contains(victim.getType())) RingRanks.progress(killer, RankTask.GREEN_BOSS, 1);

        boolean fight = victim instanceof Enemy || victim instanceof Player;
        Long low = LAST_LOW.get(killer.getUUID());
        if (fight && low != null && now - low <= LOW_WIN_WINDOW) RingRanks.progress(killer, RankTask.GREEN_LOW_WIN, 1);

        if (victim instanceof Mob mob && mob.getTarget() instanceof Player target && target != killer) {
            SpectrumMeters.add(killer, Emotion.COMPASSION, config.compassionRescue);
            if (target.getHealth() < target.getMaxHealth() * LOW_HEALTH) RingRanks.progress(killer, RankTask.INDIGO_SAVE, 1);
        }

        if (fight) {
            Deque<Long> kills = KILLS.computeIfAbsent(killer.getUUID(), uuid -> new ArrayDeque<>());
            kills.addLast(now);
            while (!kills.isEmpty() && now - kills.peekFirst() > BURST_WINDOW) kills.removeFirst();
            if (kills.size() >= BURST_KILLS) RingRanks.progress(killer, RankTask.RED_BURST, 1);
        }
    }

    private static void onMined(ServerPlayer player, BlockState state) {
        BrightestDayConfig config = config();
        if (state.is(Blocks.ANCIENT_DEBRIS)) {
            SpectrumMeters.add(player, Emotion.AVARICE, config.avariceDebris);
            RingOffers.considerOrange(player);
        } else if (state.is(BlockItemTags.DIAMOND_ORES.block())) {
            SpectrumMeters.add(player, Emotion.AVARICE, config.avariceDiamond);
            RingOffers.considerOrange(player);
        } else if (state.is(BlockItemTags.EMERALD_ORES.block())) {
            SpectrumMeters.add(player, Emotion.AVARICE, config.avariceEmerald);
        }
    }

    private static void onUseBlock(ServerPlayer player, ItemStack held) {
        boolean planting = held.is(Items.BONE_MEAL) || held.is(ItemTags.SAPLINGS) || held.is(ItemTags.VILLAGER_PLANTABLE_SEEDS);
        if (planting && ready(player, "hope_plant", 20)) SpectrumMeters.add(player, Emotion.HOPE, config().hopePlant);
    }

    public static void onTrade(ServerPlayer player) {
        SpectrumMeters.add(player, Emotion.HOPE, config().hopeTrade);
    }

    public static void onCure(ServerPlayer player) {
        SpectrumMeters.add(player, Emotion.HOPE, config().hopeCure);
        RingRanks.progress(player, RankTask.BLUE_HOPE, 1);
    }

    public static void onBreed(ServerPlayer player) {
        SpectrumMeters.add(player, Emotion.LOVE, config().loveBreed);
    }

    public static void onTame(ServerPlayer player) {
        SpectrumMeters.add(player, Emotion.LOVE, config().loveTame);
    }

    public static void onGift(ServerPlayer giver, Player receiver) {
        if (ready(giver, "love_gift", 40)) SpectrumMeters.add(giver, Emotion.LOVE, config().loveGift);
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
            if (ward != null && ward.isAlive() && guardian != null) RingRanks.progress(guardian, RankTask.SAPPHIRE_GUARD, 1);
        }

        if (server.getTickCount() % 20 != 0) return;
        boolean minute = server.getTickCount() % 1200 == 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            LanternCorps corps = PowerRingItem.getWornCorps(player).orElse(null);
            if (corps == LanternCorps.ORANGE && player.getInventory().countItem(Items.DIAMOND) >= HOARD_DIAMONDS) {
                RingRanks.complete(player, RankTask.ORANGE_HOARD);
            }
            if (corps == LanternCorps.STAR_SAPPHIRE && CorpsCombat.inBondedCombat(player)) {
                RingRanks.progress(player, RankTask.SAPPHIRE_BOND, 1);
            }
            if (minute && player.level().dimension() != Level.OVERWORLD && player.getArmorValue() < WEAK_ARMOR && !player.isSpectator()) {
                SpectrumMeters.add(player, Emotion.WILL, config().willHostileMinute);
            }
        }
    }

    private EmotionSources() {}
}
