package dev.amble.core.forge;

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
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public final class CentralPowerBattery {
    private static final int CHECK_INTERVAL = 20;
    private static final int SEEK_INTERVAL = 20;
    private static final double OFFERING_RADIUS = 3.0;
    private static final double ANNOUNCE_RADIUS = 48.0;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(CentralPowerBattery::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ServerPlayNetworking.send(handler.player, payload(server)));
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || !(level instanceof ServerLevel server) || server.dimension() != Level.OVERWORLD) return InteractionResult.PASS;
            ItemStack held = player.getMainHandItem();
            boolean slotted = !(held.getItem() instanceof PowerRingItem);
            ItemStack ring = slotted ? BrightestDayAttachments.getRing(player) : held;
            if (ring.isEmpty() || slotted && (!held.isEmpty() || player.isSecondaryUseActive())) return InteractionResult.PASS;
            WorldProgress.Battery battery = at(server, hit.getBlockPos());
            if (battery == null) return InteractionResult.PASS;
            return recharge(server, battery, player, ring, slotted);
        });
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

    private static WorldProgress.Battery at(ServerLevel level, BlockPos pos) {
        for (WorldProgress.Battery battery : WorldProgress.get(level.getServer()).batteries()) {
            BlockPos core = battery.pos();
            if (battery.active() && Math.abs(pos.getX() - core.getX()) <= 1 && Math.abs(pos.getY() - core.getY()) <= 1 && Math.abs(pos.getZ() - core.getZ()) <= 1) return battery;
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
        WorldProgress.update(level.getServer(), state -> state.withBattery(new WorldProgress.Battery(pos, corps, false)));
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
                if (whole && !wasActive) {
                    announce(level, battery.pos(), Component.translatable("message.brightestday.battery.online").withStyle(ChatFormatting.BOLD), battery.corps());
                    level.playSound(null, battery.pos(), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0F, 0.6F);
                } else if (!whole && wasActive) {
                    announce(level, battery.pos(), Component.translatable("message.brightestday.battery.offline"), battery.corps());
                }
                kept.add(battery.withActive(whole));
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
