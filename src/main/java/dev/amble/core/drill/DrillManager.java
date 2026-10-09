package dev.amble.core.drill;

import dev.amble.config.BrightestDayConfig;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.networking.payloads.s2c.DrillS2CPayload;
import dev.amble.core.ringpowers.constructs.DrillConstruct;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class DrillManager {

    static final float HARDNESS_DIVISOR = 30.0F;
    private static final int GRACE_TICKS = 8;
    private static final int HIT_SOUND_INTERVAL = 4;
    private static final int PARTICLE_INTERVAL = 2;

    private static final Map<ServerPlayer, Drill> DRILLS = new HashMap<>();

    private static final class Drill {
        final ServerLevel level;
        final int color;
        final int size;
        final Map<BlockPos, Cell> cells = new HashMap<>();
        int age;

        Drill(ServerLevel level, int color, int size) {
            this.level = level;
            this.color = color;
            this.size = size;
        }
    }

    private static final class Cell {
        float progress;
        int stage = -1;
        int idle;
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(DrillManager::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> stop(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> DRILLS.clear());
    }

    public static void start(ServerPlayer player, int size, int color) {
        if (DRILLS.containsKey(player)) return;
        DRILLS.put(player, new Drill(player.level(), color, size));
        broadcast(player, color, size, true);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 2.0F);
    }

    public static void stop(ServerPlayer player) {
        Drill drill = DRILLS.remove(player);
        if (drill == null) return;
        for (BlockPos pos : drill.cells.keySet()) drill.level.destroyBlockProgress(crackId(player, pos), pos, -1);
        broadcast(player, drill.color, drill.size, false);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7F, 2.0F);
    }

    public static boolean isDrilling(Entity entity) {
        return entity instanceof ServerPlayer player && DRILLS.containsKey(player);
    }

    private static void tick(MinecraftServer server) {
        boolean drainTick = server.getTickCount() % 20 == 0;

        for (ServerPlayer player : new ArrayList<>(DRILLS.keySet())) {
            Drill drill = DRILLS.get(player);
            boolean selected = ArmedRingPower.selectedConstruct(player).orElse(null) instanceof DrillConstruct;
            int drain = BrightestDayConfig.get().drillDrainPerSecond + BrightestDayConfig.get().drillDrainPerSize * (drill.size - 1);
            boolean outOfCharge = !PowerRingItem.hasCharge(player)
                    || drainTick && !player.hasInfiniteMaterials() && !PowerRingItem.drainWorn(player, drain);
            if (player.isRemoved() || !player.isAlive() || player.isSpectator() || player.level() != drill.level || !ArmedRingPower.isArmed(player) || !selected || outOfCharge) {
                stop(player);
                continue;
            }

            drill.age++;
            bore(player, drill);
        }
    }

    private static void bore(ServerPlayer player, Drill drill) {
        ServerLevel level = drill.level;
        Set<BlockPos> touched = new HashSet<>();
        BlockHitResult hit = DrillGeometry.target(level, player, player.getEyePosition(), player.getLookAngle());

        if (hit != null) {
            BlockState centerState = level.getBlockState(hit.getBlockPos());
            if (DrillGeometry.drillable(level, hit.getBlockPos(), centerState)) feedback(level, hit, centerState, drill);

            GameType mode = player.gameMode.getGameModeForPlayer();
            float speed = BrightestDayConfig.get().drillSpeed / DrillGeometry.side(drill.size);
            for (BlockPos pos : DrillGeometry.face(hit.getBlockPos(), hit.getDirection(), drill.size)) {
                BlockState state = level.getBlockState(pos);
                if (!canBreak(level, player, mode, pos, state)) continue;

                touched.add(pos);
                Cell cell = drill.cells.computeIfAbsent(pos, key -> new Cell());
                cell.idle = 0;
                float hardness = state.getDestroySpeed(level, pos);
                cell.progress += hardness <= 0.0F ? 1.0F : speed / hardness / HARDNESS_DIVISOR;

                if (cell.progress >= 1.0F) {
                    drill.cells.remove(pos);
                    level.destroyBlockProgress(crackId(player, pos), pos, -1);
                    breakBlock(level, player, pos, state);
                    continue;
                }

                int stage = (int) (cell.progress * 10.0F);
                if (stage != cell.stage) {
                    cell.stage = stage;
                    level.destroyBlockProgress(crackId(player, pos), pos, stage);
                }
            }
        }

        Iterator<Map.Entry<BlockPos, Cell>> iterator = drill.cells.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Cell> entry = iterator.next();
            if (touched.contains(entry.getKey()) || ++entry.getValue().idle <= GRACE_TICKS) continue;
            level.destroyBlockProgress(crackId(player, entry.getKey()), entry.getKey(), -1);
            iterator.remove();
        }
    }

    private static void feedback(ServerLevel level, BlockHitResult hit, BlockState state, Drill drill) {
        feedback(level, hit.getLocation(), state, drill.size, drill.age);
    }

    static void feedback(ServerLevel level, Vec3 point, BlockState state, int size, int age) {
        int side = DrillGeometry.side(size);
        if (age % HIT_SOUND_INTERVAL == 0) {
            SoundType sound = state.getSoundType();
            level.playSound(null, point.x, point.y, point.z, sound.getHitSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 4.0F, sound.getPitch() * 0.8F);
        }
        if (age % PARTICLE_INTERVAL == 0) {
            double spread = 0.1 + 0.15 * (side - 1);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), point.x, point.y, point.z, 3 + side, spread, spread, spread, 0.15);
        }
    }

    static boolean canBreak(ServerLevel level, ServerPlayer player, GameType mode, BlockPos pos, BlockState state) {
        if (!DrillGeometry.drillable(level, pos, state)) return false;
        if (state.getBlock() instanceof GameMasterBlock && !player.canUseGameMasterBlocks()) return false;
        return level.mayInteract(player, pos) && level.getWorldBorder().isWithinBounds(pos) && !player.blockActionRestricted(level, pos, mode);
    }

    static void breakBlock(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, blockEntity)) {
            PlayerBlockBreakEvents.CANCELED.invoker().onBlockBreakCanceled(level, player, pos, state, blockEntity);
            return;
        }

        Block block = state.getBlock();
        BlockState adjusted = block.playerWillDestroy(level, pos, state, player);
        player.connection.send(new ClientboundLevelEventPacket(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos, Block.getId(state), false));
        if (!level.removeBlock(pos, false)) return;

        block.destroy(level, pos, adjusted);
        if (!player.preventsBlockDrops()) block.playerDestroy(level, player, pos, adjusted, blockEntity, new ItemStack(Items.NETHERITE_PICKAXE));
        PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, player, pos, adjusted, blockEntity);
    }

    private static int crackId(ServerPlayer player, BlockPos pos) {
        return (Long.hashCode(pos.asLong()) * 31 + player.getId()) | Integer.MIN_VALUE;
    }

    private static void broadcast(ServerPlayer player, int color, int size, boolean active) {
        DrillS2CPayload payload = new DrillS2CPayload(player.getId(), color, size, active);
        ServerPlayNetworking.send(player, payload);
        for (ServerPlayer watcher : PlayerLookup.tracking(player)) {
            if (watcher != player) ServerPlayNetworking.send(watcher, payload);
        }
    }

    private DrillManager() {}
}
