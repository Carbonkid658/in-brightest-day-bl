package dev.amble.core.light;

import dev.amble.core.BrightestDayBlocks;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class LightManager {
    private static final Map<ServerLevel, Set<BlockPos>> OWNED = new WeakHashMap<>();
    private static final Map<ServerPlayer, Spot> SPOTS = new HashMap<>();

    private record Spot(ServerLevel level, BlockPos pos) {}

    public static void init() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clearSpot(handler.player));
    }

    public static boolean owns(ServerLevel level, BlockPos pos) {
        Set<BlockPos> owned = OWNED.get(level);
        return owned != null && owned.contains(pos);
    }

    public static boolean place(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) return false;
        level.setBlock(pos, BrightestDayBlocks.CONSTRUCT_LIGHT.defaultBlockState(), 3);
        OWNED.computeIfAbsent(level, key -> new HashSet<>()).add(pos.immutable());
        return true;
    }

    public static void remove(ServerLevel level, BlockPos pos) {
        Set<BlockPos> owned = OWNED.get(level);
        if (owned != null) owned.remove(pos);
        if (level.getBlockState(pos).is(BrightestDayBlocks.CONSTRUCT_LIGHT)) level.removeBlock(pos, false);
    }

    public static @Nullable BlockPos findAir(ServerLevel level, BlockPos origin, int reach) {
        if (level.getBlockState(origin).isAir()) return origin;
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-reach, -reach, -reach), origin.offset(reach, reach, reach))) {
            double distance = pos.distSqr(origin);
            if (distance < bestDistance && level.getBlockState(pos).isAir()) {
                best = pos.immutable();
                bestDistance = distance;
            }
        }
        return best;
    }

    public static void updateSpot(ServerPlayer player, @Nullable BlockPos target) {
        ServerLevel level = player.level();
        Spot current = SPOTS.get(player);
        if (current != null && current.level() == level && current.pos().equals(target)) return;

        clearSpot(player);
        if (target != null && level.mayInteract(player, target) && place(level, target)) SPOTS.put(player, new Spot(level, target.immutable()));
    }

    public static void clearSpot(ServerPlayer player) {
        Spot spot = SPOTS.remove(player);
        if (spot != null) remove(spot.level(), spot.pos());
    }

    private LightManager() {}
}
