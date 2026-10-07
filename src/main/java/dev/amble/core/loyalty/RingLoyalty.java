package dev.amble.core.loyalty;

import dev.amble.config.BrightestDayConfig;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.BrightestDayBlocks;
import dev.amble.core.BrightestDayComponents;
import dev.amble.core.blocks.LanternBlock;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.progression.Emotion;
import dev.amble.core.progression.SpectrumMeters;
import dev.amble.core.ringpowers.LanternCorps;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class RingLoyalty {
    private static final double SKY_ENTRY_HEIGHT = 48.0;
    private static final double SKY_ENTRY_SPREAD = 24.0;
    private static final double START_SPEED = 0.35;
    private static final double ACCELERATION = 0.04;
    private static final double MAX_SPEED = 2.5;
    private static final double ARRIVAL_DISTANCE = 1.0;
    private static final int MAX_FLIGHT_TICKS = 20 * 60;
    private static final int TRAIL_STEPS = 4;

    private static final List<Flight> FLIGHTS = new ArrayList<>();

    private static final class Flight {
        final ServerLevel level;
        Vec3 position;
        final UUID target;
        final ItemStack ring;
        final @Nullable UUID formerBearer;
        int age;

        Flight(ServerLevel level, Vec3 position, UUID target, ItemStack ring, @Nullable UUID formerBearer) {
            this.level = level;
            this.position = position;
            this.target = target;
            this.ring = ring;
            this.formerBearer = formerBearer;
        }
    }

    public static void init() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) onDeath(player);
        });
        ServerTickEvents.END_SERVER_TICK.register(RingLoyalty::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (Flight flight : List.copyOf(FLIGHTS)) land(server, flight);
            FLIGHTS.clear();
        });
    }

    public static void bind(ItemStack ring, ServerLevel level, BlockPos pos) {
        if (PowerRingItem.getCorps(ring).map(LanternCorps::hasLantern).orElse(false)) {
            ring.set(BrightestDayComponents.BOUND_LANTERN, GlobalPos.of(level.dimension(), pos));
        }
    }

    private static void onDeath(ServerPlayer player) {
        ItemStack ring = BrightestDayAttachments.getRing(player);
        if (ring.isEmpty()) return;

        int threshold = BrightestDayConfig.get().ringLoyaltyDeaths;
        if (threshold <= 0) return;

        int deaths = ring.getOrDefault(BrightestDayComponents.RING_DEATHS, 0) + 1;
        int color = corps(ring).color();
        if (deaths < threshold) {
            ring.set(BrightestDayComponents.RING_DEATHS, deaths);
            BrightestDayAttachments.setRing(player, ring);
            player.sendSystemMessage(Component.translatable("message.brightestday.loyalty_wavers", threshold - deaths).withColor(color));
            return;
        }

        BrightestDayAttachments.setRing(player, ItemStack.EMPTY);
        ring.remove(BrightestDayComponents.RING_DEATHS);
        RingBonds.release(player.level().getServer(), ring);
        player.sendSystemMessage(Component.translatable("message.brightestday.loyalty_departed").withStyle(ChatFormatting.ITALIC).withColor(color));
        depart(player.level(), player.getEyePosition(), ring, player.getUUID());
    }

    private static void depart(ServerLevel level, Vec3 origin, ItemStack ring, @Nullable UUID formerBearer) {
        MinecraftServer server = level.getServer();
        ServerPlayer target = findWorthy(server, level, origin, formerBearer, corps(ring));
        if (target == null) {
            drop(level, origin, ring);
            return;
        }

        Vec3 start = target.level() == level && target.position().distanceTo(origin) <= BrightestDayConfig.get().ringLoyaltySearchRadius
                ? origin
                : skyEntry(target);
        Flight flight = new Flight(target.level(), start, target.getUUID(), ring, formerBearer);
        FLIGHTS.add(flight);
        flight.level.playSound(null, start.x, start.y, start.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5F, 1.4F);
    }

    private static @Nullable ServerPlayer findWorthy(MinecraftServer server, ServerLevel level, Vec3 origin, @Nullable UUID exclude, LanternCorps corps) {
        double radius = BrightestDayConfig.get().ringLoyaltySearchRadius;
        Emotion emotion = Emotion.of(corps).orElse(null);
        ServerPlayer best = null;
        int bestMeter = -1;
        double bestDistance = Double.MAX_VALUE;
        for (ServerPlayer candidate : level.players()) {
            if (!isWorthy(candidate, exclude)) continue;
            double distance = candidate.position().distanceToSqr(origin);
            if (distance > radius * radius) continue;
            int meter = emotion == null ? 0 : SpectrumMeters.get(candidate, emotion);
            if (meter > bestMeter || meter == bestMeter && distance < bestDistance) {
                best = candidate;
                bestMeter = meter;
                bestDistance = distance;
            }
        }
        if (best != null) return best;

        List<ServerPlayer> elsewhere = new ArrayList<>();
        for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
            if (isWorthy(candidate, exclude)) elsewhere.add(candidate);
        }
        if (elsewhere.isEmpty()) return null;
        if (emotion != null) return elsewhere.stream().max(Comparator.comparingInt(candidate -> SpectrumMeters.get(candidate, emotion))).orElse(null);
        return elsewhere.get(level.getRandom().nextInt(elsewhere.size()));
    }

    public static void deliver(ServerPlayer target, ItemStack ring) {
        Vec3 start = skyEntry(target);
        FLIGHTS.add(new Flight(target.level(), start, target.getUUID(), ring, null));
        target.level().playSound(null, start.x, start.y, start.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5F, 1.4F);
    }

    public static boolean inFlight(LanternCorps corps) {
        return FLIGHTS.stream().anyMatch(flight -> corps(flight.ring) == corps);
    }

    private static boolean isWorthy(ServerPlayer player, @Nullable UUID exclude) {
        return !player.getUUID().equals(exclude)
                && player.isAlive()
                && !player.isSpectator()
                && BrightestDayAttachments.getRing(player).isEmpty()
                && !RingBonds.bonded(player);
    }

    private static Vec3 skyEntry(ServerPlayer target) {
        float angle = target.getRandom().nextFloat() * Mth.TWO_PI;
        return target.getEyePosition().add(Mth.cos(angle) * SKY_ENTRY_SPREAD, SKY_ENTRY_HEIGHT, Mth.sin(angle) * SKY_ENTRY_SPREAD);
    }

    private static void tick(MinecraftServer server) {
        Iterator<Flight> iterator = FLIGHTS.iterator();
        List<Flight> retargeted = new ArrayList<>();
        while (iterator.hasNext()) {
            Flight flight = iterator.next();
            ServerPlayer target = server.getPlayerList().getPlayer(flight.target);
            if (target == null || !isWorthy(target, null) || target.level() != flight.level || ++flight.age > MAX_FLIGHT_TICKS) {
                iterator.remove();
                retargeted.add(flight);
                continue;
            }

            Vec3 destination = target.getEyePosition().subtract(0.0, 0.4, 0.0);
            Vec3 path = destination.subtract(flight.position);
            double distance = path.length();
            double speed = Math.min(START_SPEED + flight.age * ACCELERATION, MAX_SPEED);
            if (distance <= Math.max(speed, ARRIVAL_DISTANCE)) {
                iterator.remove();
                arrive(target, flight.ring);
                continue;
            }

            Vec3 next = flight.position.add(path.scale(speed / distance));
            trail(flight.level, flight.position, next, corps(flight.ring).color());
            flight.position = next;
            if (flight.age % 20 == 0) {
                flight.level.playSound(null, next.x, next.y, next.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.6F);
            }
        }

        for (Flight flight : retargeted) {
            if (flight.age > MAX_FLIGHT_TICKS) drop(flight.level, flight.position, flight.ring);
            else depart(flight.level, flight.position, flight.ring, flight.formerBearer);
        }
    }

    private static void trail(ServerLevel level, Vec3 from, Vec3 to, int color) {
        DustParticleOptions dust = new DustParticleOptions(color, 1.4F);
        for (int i = 0; i < TRAIL_STEPS; i++) {
            Vec3 point = from.lerp(to, i / (double) TRAIL_STEPS);
            level.sendParticles(dust, point.x, point.y, point.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
        level.sendParticles(ParticleTypes.END_ROD, to.x, to.y, to.z, 1, 0.05, 0.05, 0.05, 0.01);
    }

    private static void land(MinecraftServer server, Flight flight) {
        ServerPlayer target = server.getPlayerList().getPlayer(flight.target);
        if (target != null && isWorthy(target, null)) arrive(target, flight.ring);
        else drop(flight.level, flight.position, flight.ring);
    }

    private static void arrive(ServerPlayer bearer, ItemStack ring) {
        LanternCorps corps = corps(ring);
        RingBonds.bind(bearer, ring);
        BrightestDayAttachments.setRing(bearer, ring);

        ServerLevel level = bearer.level();
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, bearer.getX(), bearer.getY(1.0), bearer.getZ(), 40, 0.4, 0.6, 0.4, 0.3);
        level.playSound(null, bearer.getX(), bearer.getY(), bearer.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2F, 1.0F);

        bearer.sendSystemMessage(Component.translatable("message.brightestday.loyalty_chosen", corps.displayName())
                .withStyle(ChatFormatting.BOLD)
                .withColor(corps.color()));
        if (corps.hasLantern()) revealLantern(bearer, ring, corps);
    }

    private static void revealLantern(ServerPlayer bearer, ItemStack ring, LanternCorps corps) {
        GlobalPos bound = ring.get(BrightestDayComponents.BOUND_LANTERN);
        if (bound != null && lanternStands(bearer.level().getServer(), bound, corps)) {
            BlockPos pos = bound.pos();
            bearer.sendSystemMessage(Component.translatable("message.brightestday.loyalty_lantern_found",
                    pos.getX(), pos.getY(), pos.getZ(), bound.dimension().identifier().toString()).withColor(corps.color()));
            return;
        }

        BrightestDayBlocks.lantern(corps).ifPresent(lantern -> {
            ring.remove(BrightestDayComponents.BOUND_LANTERN);
            BrightestDayAttachments.setRing(bearer, ring);

            ItemStack gift = new ItemStack(lantern);
            if (!bearer.addItem(gift)) bearer.drop(gift, false, Prediction.SERVER_ONLY);
            bearer.sendSystemMessage(Component.translatable("message.brightestday.loyalty_lantern_lost").withColor(corps.color()));
        });
    }

    private static boolean lanternStands(MinecraftServer server, GlobalPos bound, LanternCorps corps) {
        ServerLevel level = server.getLevel(bound.dimension());
        return level != null
                && level.getBlockState(bound.pos()).getBlock() instanceof LanternBlock lantern
                && lantern.corps() == corps;
    }

    private static void drop(ServerLevel level, Vec3 position, ItemStack ring) {
        ItemEntity item = new ItemEntity(level, position.x, position.y, position.z, ring);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    private static LanternCorps corps(ItemStack ring) {
        return PowerRingItem.getCorps(ring).orElse(LanternCorps.GREEN);
    }

    private RingLoyalty() {}
}
