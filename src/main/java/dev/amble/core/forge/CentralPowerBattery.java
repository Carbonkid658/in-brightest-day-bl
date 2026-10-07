package dev.amble.core.forge;

import dev.amble.config.BrightestDayConfig;
import dev.amble.core.progression.IndigoOne;
import dev.amble.core.progression.Milestone;
import dev.amble.core.progression.Trigger;
import dev.amble.core.progression.RingRanks;
import dev.amble.core.BrightestDayComponents;
import dev.amble.core.networking.payloads.s2c.BatteriesS2CPayload;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.loyalty.RingLoyalty;
import dev.amble.core.progression.Emotion;
import dev.amble.core.progression.SpectrumMeters;
import dev.amble.core.progression.WorldProgress;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.ringpowers.LanternCorps;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.BossEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CentralPowerBattery {
    private static final int CHECK_INTERVAL = 20;
    private static final int SEEK_INTERVAL = 20;
    private static final double OFFERING_RADIUS = 3.0;
    private static final double ANNOUNCE_RADIUS = 48.0;
    private static final double THREAT_RADIUS = 32.0;
    private static final int MELEE_DAMAGE = 2;
    private static final long MELEE_COOLDOWN = 10L;
    private static final int REPAIR_INTERVAL = 60;
    private static final int OFFER_COST = 50;
    private static final int OFFER_HEALTH = 20;
    private static final long CEREMONY_WINDOW = 600L;

    private static final Map<BlockPos, ServerBossEvent> BARS = new HashMap<>();
    private static final Map<BlockPos, Map<UUID, Long>> CEREMONIES = new HashMap<>();
    private static final Map<UUID, Long> LAST_STRIKE = new HashMap<>();
    private static final Set<BlockPos> AWAITING = new HashSet<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(CentralPowerBattery::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ServerPlayNetworking.send(handler.player, payload(server)));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            BARS.values().forEach(ServerBossEvent::removeAllPlayers);
            BARS.clear();
            CEREMONIES.clear();
            AWAITING.clear();
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || !(level instanceof ServerLevel server) || server.dimension() != Level.OVERWORLD) return InteractionResult.PASS;
            WorldProgress.Battery battery = at(server, hit.getBlockPos(), false);
            if (battery == null) return InteractionResult.PASS;
            boolean member = PowerRingItem.getWornCorps(player).orElse(null) == battery.corps();
            ItemStack held = player.getMainHandItem();
            if (member && player.isSecondaryUseActive() && held.isEmpty() && battery.health() < WorldProgress.Battery.MAX_HEALTH) return offer(server, battery, player);
            if (!battery.active()) return member && !player.isSecondaryUseActive() ? ceremony(server, battery, player) : InteractionResult.PASS;
            boolean slotted = !(held.getItem() instanceof PowerRingItem);
            ItemStack ring = slotted ? BrightestDayAttachments.getRing(player) : held;
            if (ring.isEmpty() || slotted && (!held.isEmpty() || player.isSecondaryUseActive())) return InteractionResult.PASS;
            return recharge(server, battery, player, ring, slotted);
        });
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (!(level instanceof ServerLevel server) || server.dimension() != Level.OVERWORLD || !(player instanceof ServerPlayer attacker)) return InteractionResult.PASS;
            WorldProgress.Battery battery = at(server, pos, false);
            if (battery == null || !hostile(attacker, battery)) return InteractionResult.PASS;
            long now = server.getGameTime();
            Long last = LAST_STRIKE.get(attacker.getUUID());
            if (last == null || now - last >= MELEE_COOLDOWN) {
                LAST_STRIKE.put(attacker.getUUID(), now);
                damage(server, battery, MELEE_DAMAGE, attacker);
            }
            return InteractionResult.PASS;
        });
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (!(level instanceof ServerLevel server) || server.dimension() != Level.OVERWORLD || player.isCreative()) return true;
            WorldProgress.Battery battery = at(server, pos, false);
            if (battery == null || PowerRingItem.getWornCorps(player).orElse(null) == battery.corps()) return true;
            player.sendOverlayMessage(Component.translatable("message.brightestday.battery.warded").withColor(battery.corps().color()));
            return false;
        });
    }

    private static boolean hostile(Player player, WorldProgress.Battery battery) {
        LanternCorps worn = PowerRingItem.getWornCorps(player).orElse(null);
        return worn != null && worn != battery.corps() && !player.isSpectator();
    }

    public static float strength(MinecraftServer server, LanternCorps corps) {
        if (!required(corps)) return 1.0F;
        float best = -1.0F;
        for (WorldProgress.Battery battery : WorldProgress.get(server).batteries()) {
            if (battery.corps() == corps && battery.active()) best = Math.max(best, battery.strength());
        }
        return best < 0.0F ? 1.0F : best;
    }

    public static void blast(ServerLevel level, Vec3 point, double radius, int amount, ServerPlayer attacker) {
        if (level.dimension() != Level.OVERWORLD) return;
        for (WorldProgress.Battery battery : WorldProgress.get(level.getServer()).batteries()) {
            if (!hostile(attacker, battery) || Vec3.atCenterOf(battery.pos()).distanceTo(point) > radius + 1.5) continue;
            damage(level, battery, amount, attacker);
        }
    }

    private static void damage(ServerLevel level, WorldProgress.Battery battery, int amount, ServerPlayer attacker) {
        if (battery.health() <= 0) return;
        WorldProgress.Battery hurt = battery.withHealth(battery.health() - amount);
        if (hurt.health() <= 0) {
            hurt = hurt.withLit(false);
            announce(level, battery.pos(), Component.translatable("message.brightestday.battery.destroyed", attacker.getDisplayName()).withStyle(ChatFormatting.BOLD), battery.corps());
            level.playSound(null, battery.pos(), SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 2.0F, 0.5F);
        }
        WorldProgress.Battery updated = hurt;
        WorldProgress.update(level.getServer(), state -> state.withBattery(updated));
        level.sendParticles(new DustParticleOptions(battery.corps().color(), 1.8F), battery.pos().getX() + 0.5, battery.pos().getY() + 0.5, battery.pos().getZ() + 0.5, 12, 0.8, 0.8, 0.8, 0.0);
        level.playSound(null, battery.pos(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1.0F, 0.6F);
        bar(level, updated);
    }

    private static InteractionResult offer(ServerLevel level, WorldProgress.Battery battery, Player player) {
        Emotion emotion = Emotion.of(battery.corps()).orElse(null);
        if (emotion == null || !(player instanceof ServerPlayer server)) return InteractionResult.PASS;
        if (SpectrumMeters.get(server, emotion) < OFFER_COST) {
            player.sendOverlayMessage(Component.translatable("message.brightestday.battery.offer_empty").withColor(battery.corps().color()));
            return InteractionResult.FAIL;
        }
        SpectrumMeters.drain(server, emotion, OFFER_COST);
        WorldProgress.Battery healed = battery.withHealth(battery.health() + OFFER_HEALTH);
        WorldProgress.update(level.getServer(), state -> state.withBattery(healed));
        level.sendParticles(new DustParticleOptions(battery.corps().color(), 2.0F), player.getX(), player.getY(1.0), player.getZ(), 20, 0.4, 0.6, 0.4, 0.0);
        level.playSound(null, battery.pos(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.2F, 1.2F);
        bar(level, healed);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static InteractionResult ceremony(ServerLevel level, WorldProgress.Battery battery, Player player) {
        BrightestDayConfig config = BrightestDayConfig.get();
        if (!config.batteryCeremony || battery.lit() || battery.health() <= 0 || !complete(level, battery.pos())) return InteractionResult.PASS;
        Emotion emotion = Emotion.of(battery.corps()).orElse(null);
        long now = level.getGameTime();
        Map<UUID, Long> sworn = CEREMONIES.computeIfAbsent(battery.pos(), pos -> new HashMap<>());
        sworn.values().removeIf(time -> now - time > CEREMONY_WINDOW);
        sworn.put(player.getUUID(), now);
        int needed = IndigoOne.multiplayer(level.getServer()) ? Math.max(1, config.batteryCeremonyMembers) : 1;
        boolean worthy = emotion != null && SpectrumMeters.get(player, emotion) >= Emotion.MAX;
        player.sendSystemMessage(Component.translatable(battery.corps().oathKey()).withStyle(ChatFormatting.BOLD).withColor(battery.corps().color()));
        if (!worthy && sworn.size() < needed) {
            announce(level, battery.pos(), Component.translatable("message.brightestday.battery.ceremony", player.getDisplayName(), sworn.size(), needed), battery.corps());
            level.playSound(null, battery.pos(), SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 1.5F, 0.8F);
            return InteractionResult.SUCCESS_SERVER;
        }
        CEREMONIES.remove(battery.pos());
        AWAITING.remove(battery.pos());
        WorldProgress.update(level.getServer(), state -> state.withBattery(battery.withLit(true)));
        announce(level, battery.pos(), Component.translatable("message.brightestday.battery.lit").withStyle(ChatFormatting.BOLD), battery.corps());
        level.playSound(null, battery.pos(), SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 2.0F, 0.7F);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void bar(ServerLevel level, WorldProgress.Battery battery) {
        ServerBossEvent bar = BARS.get(battery.pos());
        if (battery.health() >= WorldProgress.Battery.MAX_HEALTH) {
            if (bar != null) {
                bar.removeAllPlayers();
                BARS.remove(battery.pos());
            }
            return;
        }
        if (bar == null) {
            bar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("bossbar.brightestday.battery", battery.corps().displayName()).withColor(battery.corps().color()),
                    BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
            BARS.put(battery.pos(), bar);
        }
        bar.setProgress(battery.strength());
        Vec3 center = Vec3.atCenterOf(battery.pos());
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceTo(center) <= THREAT_RADIUS) bar.addPlayer(player);
            else bar.removePlayer(player);
        }
    }

    private static boolean threatened(ServerLevel level, WorldProgress.Battery battery) {
        Vec3 center = Vec3.atCenterOf(battery.pos());
        for (ServerPlayer player : level.players()) {
            if (hostile(player, battery) && player.position().distanceTo(center) <= THREAT_RADIUS) return true;
        }
        return false;
    }

    public static BatteriesS2CPayload payload(MinecraftServer server) {
        return new BatteriesS2CPayload(WorldProgress.get(server).batteries().stream()
                .filter(WorldProgress.Battery::active)
                .map(battery -> new BatteriesS2CPayload.Entry(battery.pos(), battery.corps().color()))
                .toList());
    }

    public static void broadcast(MinecraftServer server) {
        BatteriesS2CPayload payload = payload(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) ServerPlayNetworking.send(player, payload);
    }

    private static WorldProgress.Battery at(ServerLevel level, BlockPos pos, boolean activeOnly) {
        for (WorldProgress.Battery battery : WorldProgress.get(level.getServer()).batteries()) {
            BlockPos core = battery.pos();
            if ((!activeOnly || battery.active()) && Math.abs(pos.getX() - core.getX()) <= 1 && Math.abs(pos.getY() - core.getY()) <= 1 && Math.abs(pos.getZ() - core.getZ()) <= 1) return battery;
        }
        return null;
    }

    private static InteractionResult recharge(ServerLevel level, WorldProgress.Battery battery, Player player, ItemStack ring, boolean slotted) {
        LanternCorps corps = battery.corps();
        if (!ArmedRingPower.isArmed(player)) {
            player.sendOverlayMessage(Component.translatable("message.brightestday.arm_to_charge"));
            return InteractionResult.FAIL;
        }
        if (PowerRingItem.getCorps(ring).orElse(null) != corps) {
            player.sendOverlayMessage(Component.translatable("message.brightestday.wrong_lantern", corps.displayName()).withColor(corps.color()));
            return InteractionResult.FAIL;
        }
        if (PowerRingItem.getRingPower(ring) >= BrightestDayComponents.MAX_POWER) return InteractionResult.PASS;

        PowerRingItem.setMaxPower(ring);
        ring.remove(BrightestDayComponents.RING_DEATHS);
        if (player instanceof ServerPlayer server) RingRanks.fire(server, Trigger.RECHARGE, Milestone.Context.of("battery"));
        if (slotted) BrightestDayAttachments.setRing(player, ring);
        player.sendSystemMessage(Component.translatable(corps.oathKey()).withStyle(ChatFormatting.BOLD).withColor(corps.color()));
        level.playSound(null, battery.pos(), SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.4F, 0.8F);
        level.sendParticles(new DustParticleOptions(corps.color(), 2.0F), player.getX(), player.getY(1.0), player.getZ(), 30, 0.4, 0.6, 0.4, 0.0);
        return InteractionResult.SUCCESS_SERVER;
    }

    public static boolean active(MinecraftServer server, LanternCorps corps) {
        return WorldProgress.get(server).batteries().stream().anyMatch(battery -> battery.corps() == corps && battery.active());
    }

    public static boolean required(LanternCorps corps) {
        return corps == LanternCorps.YELLOW || corps == LanternCorps.STAR_SAPPHIRE;
    }

    static void track(ServerLevel level, BlockPos pos, LanternCorps corps) {
        if (level.dimension() != Level.OVERWORLD) {
            announce(level, pos, Component.translatable("message.brightestday.battery.overworld"), corps);
            return;
        }
        if (!complete(level, pos)) {
            announce(level, pos, Component.translatable("message.brightestday.battery.incomplete"), corps);
        }
        WorldProgress.update(level.getServer(), state -> state.withBattery(WorldProgress.Battery.fresh(pos, corps)));
    }

    public static boolean complete(ServerLevel level, BlockPos core) {
        if (!(level.getBlockState(core).getBlock() instanceof BatteryCoreBlock block)) return false;
        Block shell = block.shell();
        for (BlockPos pos : BlockPos.betweenClosed(core.offset(-1, -1, -1), core.offset(1, 1, 1))) {
            if (pos.equals(core)) continue;
            if (!level.getBlockState(pos).is(shell)) return false;
        }
        return true;
    }

    private static void tick(MinecraftServer server) {
        ServerLevel level = server.overworld();
        WorldProgress state = WorldProgress.get(server);
        if (server.getTickCount() % CHECK_INTERVAL == 0 && !state.batteries().isEmpty()) {
            List<WorldProgress.Battery> kept = new ArrayList<>();
            for (WorldProgress.Battery battery : state.batteries()) {
                if (!level.isLoaded(battery.pos())) {
                    kept.add(battery);
                    continue;
                }
                if (!(level.getBlockState(battery.pos()).getBlock() instanceof BatteryCoreBlock)) continue;
                boolean whole = complete(level, battery.pos());
                boolean wasActive = battery.active();
                WorldProgress.Battery current = battery;
                if (whole && current.health() < WorldProgress.Battery.MAX_HEALTH && server.getTickCount() % REPAIR_INTERVAL == 0 && !threatened(level, current)) {
                    current = current.withHealth(current.health() + 1);
                }
                boolean ceremony = BrightestDayConfig.get().batteryCeremony;
                boolean active = whole && current.health() > 0 && (!ceremony || current.lit());
                if (whole && ceremony && !current.lit() && current.health() > 0 && AWAITING.add(current.pos())) {
                    announce(level, current.pos(), Component.translatable("message.brightestday.battery.awaits"), current.corps());
                }
                if (active && !wasActive) {
                    announce(level, battery.pos(), Component.translatable("message.brightestday.battery.online").withStyle(ChatFormatting.BOLD), battery.corps());
                    level.playSound(null, battery.pos(), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0F, 0.6F);
                } else if (!active && wasActive && current.health() > 0) {
                    announce(level, battery.pos(), Component.translatable("message.brightestday.battery.offline"), battery.corps());
                }
                current = current.withActive(active);
                bar(level, current);
                kept.add(current);
            }
            if (!kept.equals(state.batteries())) {
                WorldProgress.update(server, current -> current.withBatteries(kept));
                broadcast(server);
            }
        }

        if (server.getTickCount() % SEEK_INTERVAL != 0) return;
        for (WorldProgress.Battery battery : WorldProgress.get(server).batteries()) {
            if (!battery.active() || !level.isLoaded(battery.pos())) continue;
            BlockPos pos = battery.pos();
            level.sendParticles(new DustParticleOptions(battery.corps().color(), 1.5F), pos.getX() + 0.5, pos.getY() + 1.8, pos.getZ() + 0.5, 4, 0.6, 0.3, 0.6, 0.0);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(OFFERING_RADIUS))) {
                seek(level, battery, item);
            }
        }
    }

    private static void seek(ServerLevel level, WorldProgress.Battery battery, ItemEntity item) {
        ItemStack ring = item.getItem();
        if (PowerRingItem.getCorps(ring).orElse(null) != battery.corps() || !ring.has(DataComponents.CUSTOM_NAME)) return;
        if (item.entityTags().contains("brightestday.refused")) return;

        String name = ring.getHoverName().getString();
        ServerPlayer target = level.getServer().getPlayerList().getPlayerByName(name);
        Emotion emotion = Emotion.of(battery.corps()).orElseThrow();
        boolean worthy = target != null && BrightestDayAttachments.getRing(target).isEmpty() && SpectrumMeters.passes(target, emotion);
        if (!worthy) {
            item.addTag("brightestday.refused");
            item.setDeltaMovement(0.0, 0.5, 0.0);
            announce(level, battery.pos(), Component.translatable("message.brightestday.battery.unworthy", name), battery.corps());
            return;
        }

        ItemStack sent = ring.copy();
        sent.remove(DataComponents.CUSTOM_NAME);
        sent.set(BrightestDayComponents.POWER_TYPE, Math.max(PowerRingItem.getRingPower(sent), 1));
        item.discard();
        RingLoyalty.deliver(target, sent);
        if (item.getOwner() instanceof ServerPlayer sender) RingRanks.fire(sender, Trigger.SEEK, Milestone.Context.of(target));
        announce(level, battery.pos(), Component.translatable("message.brightestday.battery.sent", name), battery.corps());
    }

    private static void announce(ServerLevel level, BlockPos pos, Component message, LanternCorps corps) {
        Component colored = message.copy().withColor(corps.color());
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().closerThan(pos, ANNOUNCE_RADIUS)) player.sendSystemMessage(colored);
        }
    }

    private CentralPowerBattery() {}
}
