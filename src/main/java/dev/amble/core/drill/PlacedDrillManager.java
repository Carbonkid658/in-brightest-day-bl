package dev.amble.core.drill;

import dev.amble.config.BrightestDayConfig;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.networking.payloads.s2c.PlacedDrillS2CPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class PlacedDrillManager {
    private static final int WATCH_INTERVAL = 10;

    private static final List<PlacedDrill> DRILLS = new ArrayList<>();
    private static int nextId;

    private static final class PlacedDrill {
        final int id;
        final ServerLevel level;
        final Direction direction;
        final Direction face;
        final int color;
        final int size;
        final UUID owner;
        final int ownerId;
        final long createdAt;
        final long expiresAt;
        final Map<BlockPos, Float> progress = new HashMap<>();
        final Set<ServerPlayer> watchers = Collections.newSetFromMap(new WeakHashMap<>());
        BlockPos head;
        int travelled;
        int age;
        int nextMove;

        PlacedDrill(int id, ServerLevel level, BlockPos head, Direction face, int color, int size, ServerPlayer owner) {
            this.id = id;
            this.level = level;
            this.head = head;
            this.face = face;
            this.direction = face.getOpposite();
            this.color = color;
            this.size = size;
            this.owner = owner.getUUID();
            this.ownerId = owner.getId();
            this.createdAt = level.getGameTime();
            this.expiresAt = this.createdAt + BrightestDayConfig.get().drillPlacedLifetimeTicks;
        }

        PlacedDrillS2CPayload payload(boolean present) {
            return new PlacedDrillS2CPayload(this.id, this.ownerId, this.head, this.direction, this.color, this.size, present);
        }

        Vec3 tip() {
            return Vec3.atCenterOf(this.head).add(Vec3.atLowerCornerOf(this.face.getUnitVec3i()).scale(0.5));
        }
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(PlacedDrillManager::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> DRILLS.clear());
    }

    public static boolean place(ServerPlayer player, int size, int color) {
        ServerLevel level = player.level();
        BlockHitResult hit = DrillGeometry.target(level, player, player.getEyePosition(), player.getLookAngle());
        if (hit == null || !level.mayInteract(player, hit.getBlockPos())) return false;

        while (count(player.getUUID()) >= Math.max(BrightestDayConfig.get().drillPlacedMaxCount, 1)) {
            PlacedDrill oldest = oldest(player.getUUID());
            if (oldest == null) break;
            dissolve(oldest);
        }

        PlacedDrill drill = new PlacedDrill(nextId++, level, hit.getBlockPos(), hit.getDirection(), color, size, player);
        DRILLS.add(drill);
        syncWatchers(drill);
        Vec3 tip = drill.tip();
        level.playSound(null, tip.x, tip.y, tip.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 2.0F);
        level.playSound(null, tip.x, tip.y, tip.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        return true;
    }

    public static long latestCreatedAt(UUID owner) {
        return DRILLS.stream().filter(drill -> drill.owner.equals(owner)).mapToLong(drill -> drill.createdAt).max().orElse(Long.MIN_VALUE);
    }

    public static boolean dismissLatest(UUID owner) {
        PlacedDrill newest = newest(owner);
        if (newest == null) return false;
        dissolve(newest);
        return true;
    }

    public static void dismissAll(UUID owner) {
        for (PlacedDrill drill : List.copyOf(DRILLS)) {
            if (drill.owner.equals(owner)) dissolve(drill);
        }
    }

    private static void tick(MinecraftServer server) {
        boolean drainTick = server.getTickCount() % 20 == 0;
        BrightestDayConfig config = BrightestDayConfig.get();

        for (PlacedDrill drill : List.copyOf(DRILLS)) {
            ServerPlayer owner = server.getPlayerList().getPlayer(drill.owner);
            int drain = config.drillDrainPerSecond + config.drillDrainPerSize * (drill.size - 1);
            boolean outOfCharge = owner != null && !owner.hasInfiniteMaterials()
                    && (!PowerRingItem.hasCharge(owner) || drainTick && !PowerRingItem.drainWorn(owner, drain));
            if (owner == null || outOfCharge || drill.level.getGameTime() >= drill.expiresAt) {
                dissolve(drill);
                continue;
            }

            drill.age++;
            bore(drill, owner);
        }
        if (server.getTickCount() % WATCH_INTERVAL == 0) DRILLS.forEach(PlacedDrillManager::syncWatchers);
    }

    private static void bore(PlacedDrill drill, ServerPlayer owner) {
        ServerLevel level = drill.level;
        GameType mode = owner.gameMode.getGameModeForPlayer();
        float speed = BrightestDayConfig.get().drillSpeed / DrillGeometry.side(drill.size);
        Set<BlockPos> touched = new HashSet<>();
        BlockState fed = null;

        for (BlockPos pos : DrillGeometry.face(drill.head, drill.face, drill.size)) {
            BlockState state = level.getBlockState(pos);
            if (!DrillManager.canBreak(level, owner, mode, pos, state)) continue;

            touched.add(pos);
            if (fed == null) fed = state;
            float hardness = state.getDestroySpeed(level, pos);
            float progress = drill.progress.getOrDefault(pos, 0.0F) + (hardness <= 0.0F ? 1.0F : speed / hardness / DrillManager.HARDNESS_DIVISOR);
            if (progress >= 1.0F) {
                drill.progress.remove(pos);
                level.destroyBlockProgress(crackId(drill, pos), pos, -1);
                DrillManager.breakBlock(level, owner, pos, state);
                continue;
            }

            int stage = (int) (progress * 10.0F);
            if (stage != (int) (drill.progress.getOrDefault(pos, -0.1F) * 10.0F)) level.destroyBlockProgress(crackId(drill, pos), pos, stage);
            drill.progress.put(pos, progress);
        }

        for (BlockPos pos : List.copyOf(drill.progress.keySet())) {
            if (touched.contains(pos)) continue;
            drill.progress.remove(pos);
            level.destroyBlockProgress(crackId(drill, pos), pos, -1);
        }

        if (fed != null) {
            DrillManager.feedback(level, drill.tip(), fed, drill.size, drill.age);
            return;
        }

        if (!level.getBlockState(drill.head).getCollisionShape(level, drill.head).isEmpty()) {
            dissolve(drill);
            return;
        }
        if (drill.age >= drill.nextMove) advance(drill);
    }

    private static void advance(PlacedDrill drill) {
        BlockPos next = drill.head.relative(drill.direction);
        if (++drill.travelled > BrightestDayConfig.get().drillPlacedDistance || !drill.level.isLoaded(next) || !drill.level.getWorldBorder().isWithinBounds(next)) {
            dissolve(drill);
            return;
        }

        drill.head = next;
        drill.nextMove = drill.age + Math.max(BrightestDayConfig.get().drillPlacedTravelTicks, 1);
        PlacedDrillS2CPayload update = drill.payload(true);
        for (ServerPlayer watcher : drill.watchers) {
            if (!watcher.hasDisconnected()) ServerPlayNetworking.send(watcher, update);
        }
    }

    private static int crackId(PlacedDrill drill, BlockPos pos) {
        return (Long.hashCode(pos.asLong()) * 31 + drill.id * 17 + 0x2C1B3C6D) | Integer.MIN_VALUE;
    }

    private static int count(UUID owner) {
        return (int) DRILLS.stream().filter(drill -> drill.owner.equals(owner)).count();
    }

    private static @Nullable PlacedDrill oldest(UUID owner) {
        return DRILLS.stream().filter(drill -> drill.owner.equals(owner)).findFirst().orElse(null);
    }

    private static @Nullable PlacedDrill newest(UUID owner) {
        PlacedDrill newest = null;
        for (PlacedDrill drill : DRILLS) {
            if (drill.owner.equals(owner)) newest = drill;
        }
        return newest;
    }

    private static void dissolve(PlacedDrill drill) {
        DRILLS.remove(drill);
        for (BlockPos pos : drill.progress.keySet()) drill.level.destroyBlockProgress(crackId(drill, pos), pos, -1);
        drill.progress.clear();
        PlacedDrillS2CPayload removal = drill.payload(false);
        for (ServerPlayer watcher : drill.watchers) {
            if (!watcher.hasDisconnected()) ServerPlayNetworking.send(watcher, removal);
        }
        drill.watchers.clear();
        Vec3 tip = drill.tip();
        drill.level.playSound(null, tip.x, tip.y, tip.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8F, 1.4F);
    }

    private static void syncWatchers(PlacedDrill drill) {
        Set<ServerPlayer> tracking = new HashSet<>(PlayerLookup.tracking(drill.level, drill.head));
        PlacedDrillS2CPayload spawn = drill.payload(true);
        for (ServerPlayer player : tracking) {
            if (drill.watchers.add(player)) ServerPlayNetworking.send(player, spawn);
        }
        PlacedDrillS2CPayload removal = drill.payload(false);
        drill.watchers.removeIf(player -> {
            boolean gone = !tracking.contains(player);
            if (gone && !player.hasDisconnected()) ServerPlayNetworking.send(player, removal);
            return gone;
        });
    }

    private PlacedDrillManager() {}
}
