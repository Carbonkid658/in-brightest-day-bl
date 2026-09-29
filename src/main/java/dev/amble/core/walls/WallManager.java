package dev.amble.core.walls;

import dev.amble.core.BrightestDayBlocks;
import dev.amble.core.networking.payloads.s2c.WallRemoveS2CPayload;
import dev.amble.core.networking.payloads.s2c.WallSpawnS2CPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class WallManager {
    public static final int WALL_TICKS = 400;

    private static final List<Wall> WALLS = new ArrayList<>();
    private static int nextId;

    private static final class Wall {
        final int id;
        final ServerLevel level;
        final List<BlockPos> cells;
        final UUID caster;
        final int color;
        final long createdAt;
        final Set<ServerPlayer> watchers = Collections.newSetFromMap(new WeakHashMap<>());
        int age;

        Wall(int id, ServerLevel level, List<BlockPos> cells, UUID caster, int color) {
            this.id = id;
            this.level = level;
            this.cells = cells;
            this.caster = caster;
            this.color = color;
            this.createdAt = level.getGameTime();
        }
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(WallManager::tick);
    }

    public static boolean raise(ServerLevel level, List<BlockPos> candidates, int color, ServerPlayer caster) {
        List<BlockPos> placed = new ArrayList<>();
        for (BlockPos pos : candidates) {
            if (!level.getBlockState(pos).isAir() || !level.mayInteract(caster, pos)) continue;
            level.setBlock(pos, BrightestDayBlocks.HARD_LIGHT.defaultBlockState(), 3);
            placed.add(pos.immutable());
        }
        if (placed.isEmpty()) return false;

        Wall wall = new Wall(nextId++, level, placed, caster.getUUID(), color);
        WALLS.add(wall);
        syncWatchers(wall);
        return true;
    }

    public static boolean owns(ServerLevel level, BlockPos pos) {
        for (Wall wall : WALLS) {
            if (wall.level == level && wall.cells.contains(pos)) return true;
        }
        return false;
    }

    public static long latestCreatedAt(UUID caster) {
        return WALLS.stream().filter(wall -> wall.caster.equals(caster)).mapToLong(wall -> wall.createdAt).max().orElse(Long.MIN_VALUE);
    }

    public static boolean dismissLatest(UUID caster) {
        Wall latest = null;
        for (Wall wall : WALLS) {
            if (wall.caster.equals(caster) && (latest == null || wall.id > latest.id)) latest = wall;
        }
        if (latest == null) return false;

        WALLS.remove(latest);
        collapse(latest);
        return true;
    }

    public static void dismissAll(UUID caster) {
        Iterator<Wall> iterator = WALLS.iterator();
        while (iterator.hasNext()) {
            Wall wall = iterator.next();
            if (!wall.caster.equals(caster)) continue;
            iterator.remove();
            collapse(wall);
        }
    }

    private static void tick(MinecraftServer server) {
        Iterator<Wall> iterator = WALLS.iterator();
        while (iterator.hasNext()) {
            Wall wall = iterator.next();
            if (++wall.age >= WALL_TICKS) {
                iterator.remove();
                collapse(wall);
                continue;
            }
            syncWatchers(wall);
        }
    }

    private static void collapse(Wall wall) {
        for (BlockPos pos : wall.cells) {
            if (wall.level.getBlockState(pos).is(BrightestDayBlocks.HARD_LIGHT)) wall.level.removeBlock(pos, false);
        }
        for (ServerPlayer player : wall.watchers) {
            if (!player.hasDisconnected()) ServerPlayNetworking.send(player, new WallRemoveS2CPayload(wall.id));
        }
        wall.watchers.clear();

        BlockPos center = wall.cells.getFirst();
        wall.level.playSound(null, center, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 0.9F);
    }

    private static void syncWatchers(Wall wall) {
        Set<ServerPlayer> tracking = new HashSet<>(PlayerLookup.tracking(wall.level, wall.cells.getFirst()));
        for (ServerPlayer player : tracking) {
            if (wall.watchers.add(player)) {
                ServerPlayNetworking.send(player, new WallSpawnS2CPayload(wall.id, wall.cells, wall.color, WALL_TICKS, wall.age));
            }
        }
        wall.watchers.removeIf(player -> {
            boolean gone = !tracking.contains(player);
            if (gone && !player.hasDisconnected()) ServerPlayNetworking.send(player, new WallRemoveS2CPayload(wall.id));
            return gone;
        });
    }

    private WallManager() {}
}
