package dev.amble.core.attacks.utility;

import dev.amble.core.networking.payloads.s2c.LumberjackS2CPayload;
import dev.amble.core.ringpowers.ActiveConstructs;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class LumberjackManager {
    private static final double AIM_RANGE = 8.0;
    private static final int MAX_LOGS = 256;
    private static final int HORIZONTAL_RADIUS = 8;
    private static final int MIN_LEAVES = 3;
    private static final boolean CLEAR_LEAVES = false;
    private static final int MAX_LEAVES = 512;
    private static final int LEAF_REACH = 3;
    private static final int LEAVES_PER_TICK = 24;
    private static final int WINDUP_TICKS = 8;
    private static final int FELL_TICKS = 40;
    private static final int LINGER_TICKS = 8;
    private static final int CHOP_INTERVAL = 5;
    private static final double ANCHOR_OFFSET = 1.1;

    private static final List<Job> JOBS = new ArrayList<>();
    private static int nextId;

    private static final class Job {
        final int id;
        final ServerLevel level;
        final UUID owner;
        final int ownerId;
        final int color;
        final BlockPos base;
        final Vec3 anchor;
        final float yaw;
        final List<BlockPos> logs;
        final List<BlockPos> leaves;
        final long createdAt;
        final int duration;
        final Set<ServerPlayer> watchers = Collections.newSetFromMap(new WeakHashMap<>());
        int age;
        int broken;
        int cleared;

        Job(int id, ServerPlayer owner, int color, BlockPos base, Vec3 anchor, float yaw, List<BlockPos> logs, List<BlockPos> leaves) {
            this.id = id;
            this.level = owner.level();
            this.owner = owner.getUUID();
            this.ownerId = owner.getId();
            this.color = color;
            this.base = base;
            this.anchor = anchor;
            this.yaw = yaw;
            this.logs = logs;
            this.leaves = leaves;
            this.createdAt = this.level.getGameTime();
            this.duration = WINDUP_TICKS + FELL_TICKS + Mth.ceil((float) leaves.size() / LEAVES_PER_TICK) + LINGER_TICKS;
        }
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(LumberjackManager::tick);
        ServerPlayerEvents.LEAVE.register(player -> dismissAll(player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> JOBS.clear());
    }

    public static @Nullable Component fell(ServerPlayer player, int color) {
        ServerLevel level = player.level();
        Vec3 eye = player.getEyePosition();
        HitResult hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(AIM_RANGE)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK || !level.getBlockState(blockHit.getBlockPos()).is(BlockTags.LOGS)) {
            return Component.translatable("message.brightestday.lumberjack_no_log");
        }

        BlockPos base = blockHit.getBlockPos();
        while (base.getY() > level.getMinY() && level.getBlockState(base.below()).is(BlockTags.LOGS)) base = base.below();

        GameType mode = player.gameMode.getGameModeForPlayer();
        if (!canBreak(level, player, mode, base, level.getBlockState(base))) return Component.translatable("message.brightestday.lumberjack_blocked");
        for (Job job : JOBS) {
            if (job.level == level && job.logs.subList(job.broken, job.logs.size()).contains(base)) return Component.translatable("message.brightestday.lumberjack_busy");
        }

        Set<BlockPos> naturalLeaves = new HashSet<>();
        List<BlockPos> logs = collectLogs(level, base, naturalLeaves);
        if (naturalLeaves.size() < MIN_LEAVES) return Component.translatable("message.brightestday.lumberjack_not_tree");

        BlockPos origin = base;
        logs.sort(Comparator.comparingInt((BlockPos pos) -> pos.getY()).thenComparingInt(pos -> pos.distManhattan(origin)));
        List<BlockPos> leaves = CLEAR_LEAVES ? collectLeaves(level, base, naturalLeaves) : List.of();

        Vec3 trunk = Vec3.atBottomCenterOf(base);
        Vec3 toPlayer = new Vec3(player.getX() - trunk.x, 0.0, player.getZ() - trunk.z);
        toPlayer = toPlayer.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : toPlayer.normalize();
        Vec3 anchor = trunk.add(toPlayer.scale(ANCHOR_OFFSET)).add(0.0, 0.9, 0.0);
        float yaw = (float) Mth.atan2(-toPlayer.x, -toPlayer.z);

        Job job = new Job(nextId++, player, color, base, anchor, yaw, logs, leaves);
        JOBS.add(job);
        ActiveConstructs.track(player, job);
        job.watchers.addAll(PlayerLookup.tracking(level, base));
        job.watchers.add(player);
        LumberjackS2CPayload spawn = new LumberjackS2CPayload(job.id, job.ownerId, anchor, yaw, color, job.duration, true);
        for (ServerPlayer watcher : job.watchers) ServerPlayNetworking.send(watcher, spawn);
        level.playSound(null, anchor.x, anchor.y, anchor.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.9F);
        level.playSound(null, anchor.x, anchor.y, anchor.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9F, 1.3F);
        return null;
    }

    public static long latestCreatedAt(UUID owner) {
        return JOBS.stream().filter(job -> job.owner.equals(owner)).mapToLong(job -> job.createdAt).max().orElse(Long.MIN_VALUE);
    }

    public static boolean dismissLatest(UUID owner) {
        Job newest = null;
        for (Job job : JOBS) {
            if (job.owner.equals(owner)) newest = job;
        }
        if (newest == null) return false;
        dissolve(newest);
        return true;
    }

    public static void dismissAll(UUID owner) {
        for (Job job : List.copyOf(JOBS)) {
            if (job.owner.equals(owner)) dissolve(job);
        }
    }

    private static List<BlockPos> collectLogs(ServerLevel level, BlockPos base, Set<BlockPos> naturalLeaves) {
        List<BlockPos> logs = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        visited.add(base);
        queue.add(base);
        while (!queue.isEmpty() && logs.size() < MAX_LOGS) {
            BlockPos pos = queue.poll();
            logs.add(pos);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos next = pos.offset(dx, dy, dz);
                        if (next.getY() < base.getY() || Math.abs(next.getX() - base.getX()) > HORIZONTAL_RADIUS || Math.abs(next.getZ() - base.getZ()) > HORIZONTAL_RADIUS) continue;
                        BlockState state = level.getBlockState(next);
                        if (natural(state)) {
                            naturalLeaves.add(next);
                        } else if (state.is(BlockTags.LOGS) && visited.add(next)) {
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return logs;
    }

    private static List<BlockPos> collectLeaves(ServerLevel level, BlockPos base, Set<BlockPos> seeds) {
        int radius = HORIZONTAL_RADIUS + LEAF_REACH;
        List<BlockPos> leaves = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>(seeds);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(seeds);
        while (!queue.isEmpty() && leaves.size() < MAX_LEAVES) {
            BlockPos pos = queue.poll();
            leaves.add(pos);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos next = pos.offset(dx, dy, dz);
                        if (next.getY() < base.getY() || Math.abs(next.getX() - base.getX()) > radius || Math.abs(next.getZ() - base.getZ()) > radius) continue;
                        if (natural(level.getBlockState(next)) && visited.add(next)) queue.add(next);
                    }
                }
            }
        }
        leaves.sort(Comparator.comparingInt((BlockPos pos) -> pos.getY()));
        return leaves;
    }

    private static boolean natural(BlockState state) {
        if (state.is(BlockTags.WART_BLOCKS) || state.is(Blocks.SHROOMLIGHT)) return true;
        return state.is(BlockTags.LEAVES) && state.hasProperty(LeavesBlock.PERSISTENT) && !state.getValue(LeavesBlock.PERSISTENT);
    }

    private static void tick(MinecraftServer server) {
        for (Job job : List.copyOf(JOBS)) {
            ServerPlayer owner = server.getPlayerList().getPlayer(job.owner);
            if (owner == null || owner.level() != job.level) {
                dissolve(job);
                continue;
            }

            job.age++;
            if (job.age <= WINDUP_TICKS) continue;

            int felling = job.age - WINDUP_TICKS;
            if (job.broken < job.logs.size()) {
                int target = Math.min(Mth.ceil(job.logs.size() * Math.min(felling / (float) FELL_TICKS, 1.0F)), job.logs.size());
                GameType mode = owner.gameMode.getGameModeForPlayer();
                while (job.broken < target) {
                    BlockPos pos = job.logs.get(job.broken++);
                    BlockState state = job.level.getBlockState(pos);
                    if (state.is(BlockTags.LOGS) && canBreak(job.level, owner, mode, pos, state)) breakBlock(job.level, owner, pos, state);
                }
                if (felling % CHOP_INTERVAL == 0) chop(job);
            } else if (job.cleared < job.leaves.size()) {
                GameType mode = owner.gameMode.getGameModeForPlayer();
                int target = Math.min(job.cleared + LEAVES_PER_TICK, job.leaves.size());
                while (job.cleared < target) {
                    BlockPos pos = job.leaves.get(job.cleared++);
                    BlockState state = job.level.getBlockState(pos);
                    if (natural(state) && canBreak(job.level, owner, mode, pos, state)) breakBlock(job.level, owner, pos, state);
                }
            }

            if (job.age >= job.duration) dissolve(job);
        }
    }

    private static void chop(Job job) {
        BlockState state = job.level.getBlockState(job.base);
        if (state.isAir()) state = job.level.getBlockState(job.logs.getLast());
        SoundType sound = state.getSoundType();
        Vec3 point = Vec3.atCenterOf(job.base);
        job.level.playSound(null, point.x, point.y, point.z, sound.getHitSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        job.level.playSound(null, point.x, point.y, point.z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.5F, 1.6F);
        if (!state.isAir()) job.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), point.x, point.y, point.z, 8, 0.3, 0.3, 0.3, 0.15);
    }

    private static boolean canBreak(ServerLevel level, ServerPlayer player, GameType mode, BlockPos pos, BlockState state) {
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) return false;
        if (state.getBlock() instanceof GameMasterBlock && !player.canUseGameMasterBlocks()) return false;
        return level.mayInteract(player, pos) && level.getWorldBorder().isWithinBounds(pos) && !player.blockActionRestricted(level, pos, mode);
    }

    private static void breakBlock(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, blockEntity)) {
            PlayerBlockBreakEvents.CANCELED.invoker().onBlockBreakCanceled(level, player, pos, state, blockEntity);
            return;
        }

        Block block = state.getBlock();
        BlockState adjusted = block.playerWillDestroy(level, pos, state, player);
        level.levelEvent(null, LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos, Block.getId(state));
        if (!level.removeBlock(pos, false)) return;

        block.destroy(level, pos, adjusted);
        if (!player.preventsBlockDrops()) block.playerDestroy(level, player, pos, adjusted, blockEntity, new ItemStack(Items.NETHERITE_AXE));
        PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, player, pos, adjusted, blockEntity);
    }

    private static void dissolve(Job job) {
        if (!JOBS.remove(job)) return;
        ActiveConstructs.untrack(job.owner, job);
        LumberjackS2CPayload removal = new LumberjackS2CPayload(job.id, job.ownerId, job.anchor, job.yaw, job.color, 0, false);
        for (ServerPlayer watcher : job.watchers) {
            if (!watcher.hasDisconnected()) ServerPlayNetworking.send(watcher, removal);
        }
        job.watchers.clear();
        job.level.playSound(null, job.anchor.x, job.anchor.y, job.anchor.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8F, 1.4F);
    }

    private LumberjackManager() {}
}
