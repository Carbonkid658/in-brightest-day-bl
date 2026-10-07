package dev.amble.core.forge;

import dev.amble.core.networking.payloads.s2c.ForgeBeatS2CPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ForgeHammer {
    public static final int STRIKES = 5;
    public static final int BEAT_TICKS = 24;
    private static final int MAX_MISSES = 2;
    private static final int WINDOW = 3;
    private static final int GAP = 8;
    private static final double REACH = 8.0;

    private static final class Session {
        final ServerLevel level;
        final BlockPos pos;
        final ForgeRecipe recipe;
        final int color;
        final boolean lava;
        long beatAt;
        int strike;
        int hits;
        int misses;

        Session(ServerLevel level, BlockPos pos, ForgeRecipe recipe, int color, boolean lava, long beatAt) {
            this.level = level;
            this.pos = pos;
            this.recipe = recipe;
            this.color = color;
            this.lava = lava;
            this.beatAt = beatAt;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(ForgeHammer::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SESSIONS.remove(handler.player.getUUID()));
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (!(player instanceof ServerPlayer smith)) return InteractionResult.PASS;
            Session session = SESSIONS.get(smith.getUUID());
            if (session == null || !session.pos.equals(pos)) return InteractionResult.PASS;
            strike(smith, session);
            return InteractionResult.SUCCESS;
        });
    }

    public static boolean forging(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    public static void begin(ServerPlayer player, ServerLevel level, BlockPos pos, ForgeRecipe recipe, int color, boolean lava) {
        Session session = new Session(level, pos.immutable(), recipe, color, lava, level.getGameTime() + GAP + BEAT_TICKS);
        SESSIONS.put(player.getUUID(), session);
        player.sendOverlayMessage(Component.translatable("forge.brightestday.hammer.begin").withColor(color));
        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
        sync(player, session, ForgeBeatS2CPayload.NONE_FEEDBACK);
    }

    private static void strike(ServerPlayer player, Session session) {
        long offset = session.level.getGameTime() - session.beatAt;
        boolean hit = Math.abs(offset) <= WINDOW;
        if (hit) {
            session.hits++;
            session.level.playSound(null, session.pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.9F, 0.9F + session.hits * 0.08F);
            Vec3 top = Vec3.atCenterOf(session.pos).add(0.0, 0.6, 0.0);
            session.level.sendParticles(ParticleTypes.CRIT, top.x, top.y, top.z, 10, 0.2, 0.1, 0.2, 0.3);
            session.level.sendParticles(new DustParticleOptions(session.color, 1.4F), top.x, top.y, top.z, 8, 0.3, 0.1, 0.3, 0.0);
        } else {
            session.misses++;
            session.level.playSound(null, session.pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5F, 0.6F);
        }
        advance(player, session, hit ? ForgeBeatS2CPayload.HIT : ForgeBeatS2CPayload.MISS);
    }

    private static void advance(ServerPlayer player, Session session, int feedback) {
        session.strike++;
        if (session.misses > MAX_MISSES) {
            fail(player, session);
            return;
        }
        if (session.strike >= STRIKES) {
            SESSIONS.remove(player.getUUID());
            ServerPlayNetworking.send(player, new ForgeBeatS2CPayload(false, 0L, session.strike, session.hits, session.misses, session.color, feedback));
            SpectrumForgeBlock.craft(session.level, session.pos, session.recipe, player, session.color, session.lava);
            return;
        }
        session.beatAt = session.level.getGameTime() + GAP + BEAT_TICKS;
        sync(player, session, feedback);
    }

    private static void fail(ServerPlayer player, Session session) {
        SESSIONS.remove(player.getUUID());
        ServerPlayNetworking.send(player, new ForgeBeatS2CPayload(false, 0L, session.strike, session.hits, session.misses, session.color, ForgeBeatS2CPayload.MISS));
        Vec3 top = Vec3.atCenterOf(session.pos).add(0.0, 0.7, 0.0);
        for (ItemStack input : session.recipe.inputs()) {
            if (!Catalysts.is(input)) continue;
            ItemEntity catalyst = new ItemEntity(session.level, top.x, top.y, top.z, input.copy());
            catalyst.setDeltaMovement(0.0, 0.2, 0.0);
            session.level.addFreshEntity(catalyst);
        }
        session.level.sendParticles(ParticleTypes.LARGE_SMOKE, top.x, top.y, top.z, 20, 0.3, 0.2, 0.3, 0.02);
        session.level.playSound(null, session.pos, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 1.0F, 0.8F);
        player.sendOverlayMessage(Component.translatable("forge.brightestday.hammer.failed").withColor(session.color));
    }

    private static void sync(ServerPlayer player, Session session, int feedback) {
        ServerPlayNetworking.send(player, new ForgeBeatS2CPayload(true, session.beatAt, session.strike, session.hits, session.misses, session.color, feedback));
    }

    private static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) return;
        for (Map.Entry<UUID, Session> entry : Map.copyOf(SESSIONS).entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Session session = entry.getValue();
            if (player == null) {
                SESSIONS.remove(entry.getKey());
                continue;
            }
            if (player.level() != session.level || player.position().distanceTo(Vec3.atCenterOf(session.pos)) > REACH
                    || !(session.level.getBlockState(session.pos).getBlock() instanceof SpectrumForgeBlock)) {
                fail(player, session);
                continue;
            }
            if (session.level.getGameTime() > session.beatAt + WINDOW) {
                session.misses++;
                session.level.playSound(null, session.pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.4F, 0.5F);
                advance(player, session, ForgeBeatS2CPayload.MISS);
            }
        }
    }

    private ForgeHammer() {}
}
