package dev.amble.core.progression;

import dev.amble.config.BrightestDayConfig;
import dev.amble.core.BrightestDayBlocks;
import dev.amble.core.ringpowers.LanternCorps;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class ZamaronMeteor {
    private static final int PINK = LanternCorps.STAR_SAPPHIRE.color();
    private static final int FLIGHT_TICKS = 140;
    private static final int MIN_DISTANCE = 300;
    private static final int MAX_DISTANCE = 800;
    private static final double APPROACH = 220.0;
    private static final double ALTITUDE = 200.0;
    private static final int CRATER_RADIUS = 5;
    private static final long NIGHT_START = 13000;
    private static final long NIGHT_END = 22000;

    private record Fall(Vec3 start, BlockPos target, int age) {}

    private static @Nullable Fall falling;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(ZamaronMeteor::tick);
    }

    private static void tick(MinecraftServer server) {
        ServerLevel level = server.overworld();
        if (falling != null) {
            fly(level);
            return;
        }
        if (server.getTickCount() % 100 != 0 || WorldProgress.get(server).meteorFallen()) return;

        long day = level.getOverworldClockTime() / 24000L;
        long time = level.getOverworldClockTime() % 24000L;
        if (day < BrightestDayConfig.get().meteorNight - 1 || time < NIGHT_START || time > NIGHT_END) return;
        launch(level);
    }

    private static void launch(ServerLevel level) {
        RandomSource random = level.getRandom();
        BlockPos spawn = level.getRespawnData().globalPos().pos();
        float angle = random.nextFloat() * Mth.TWO_PI;
        int distance = Mth.nextInt(random, MIN_DISTANCE, MAX_DISTANCE);
        drop(level, spawn.offset(Mth.floor(Mth.cos(angle) * distance), 0, Mth.floor(Mth.sin(angle) * distance)));
    }

    public static boolean falling() {
        return falling != null;
    }

    public static void drop(ServerLevel level, BlockPos target) {
        float angle = level.getRandom().nextFloat() * Mth.TWO_PI;
        Vec3 start = new Vec3(target.getX() - Mth.cos(angle) * APPROACH, level.getMaxY() + ALTITUDE * 0.25, target.getZ() - Mth.sin(angle) * APPROACH);
        falling = new Fall(start, target, 0);
        WorldProgress.update(level.getServer(), state -> state.withMeteorFallen());

        Component message = Component.translatable("message.brightestday.meteor.streak").withStyle(ChatFormatting.ITALIC).withColor(PINK);
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(message);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, SoundSource.AMBIENT, 2.0F, 0.5F);
        }
    }

    private static void fly(ServerLevel level) {
        Fall fall = falling;
        int age = fall.age() + 1;
        level.getChunk(fall.target().getX() >> 4, fall.target().getZ() >> 4);
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, fall.target().getX(), fall.target().getZ());
        Vec3 impact = new Vec3(fall.target().getX() + 0.5, groundY, fall.target().getZ() + 0.5);
        float t = age / (float) FLIGHT_TICKS;
        Vec3 position = fall.start().lerp(impact, t * t);

        DustParticleOptions dust = new DustParticleOptions(PINK, 4.0F);
        level.sendParticles(dust, true, true, position.x, position.y, position.z, 12, 1.2, 1.2, 1.2, 0.0);
        level.sendParticles(ParticleTypes.FLAME, true, true, position.x, position.y, position.z, 8, 0.8, 0.8, 0.8, 0.05);
        level.sendParticles(ParticleTypes.END_ROD, true, true, position.x, position.y, position.z, 4, 1.5, 1.5, 1.5, 0.02);

        if (age < FLIGHT_TICKS) {
            falling = new Fall(fall.start(), fall.target(), age);
            return;
        }
        falling = null;
        crater(level, BlockPos.containing(impact));
    }

    private static void crater(ServerLevel level, BlockPos impact) {
        RandomSource random = level.getRandom();
        for (BlockPos pos : BlockPos.betweenClosed(impact.offset(-CRATER_RADIUS, -CRATER_RADIUS, -CRATER_RADIUS), impact.offset(CRATER_RADIUS, CRATER_RADIUS, CRATER_RADIUS))) {
            double distance = Math.sqrt(pos.distSqr(impact.above(2)));
            if (distance <= CRATER_RADIUS) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            } else if (distance <= CRATER_RADIUS + 1.0 && pos.getY() <= impact.getY() && !level.getBlockState(pos).isAir()) {
                level.setBlockAndUpdate(pos, random.nextFloat() < 0.3F ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState());
            }
        }
        BlockPos heart = impact.below(CRATER_RADIUS - 2);
        level.setBlockAndUpdate(heart.below(), Blocks.CRYING_OBSIDIAN.defaultBlockState());
        level.setBlockAndUpdate(heart, BrightestDayBlocks.ZAMARONIAN_CRYSTAL.defaultBlockState());

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impact.getX(), impact.getY(), impact.getZ(), 3, 2.0, 1.0, 2.0, 0.0);
        level.sendParticles(new DustParticleOptions(PINK, 4.0F), impact.getX(), impact.getY() + 1, impact.getZ(), 200, 4.0, 3.0, 4.0, 0.0);
        level.playSound(null, impact, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.AMBIENT, 6.0F, 0.5F);
        Component message = Component.translatable("message.brightestday.meteor.landed").withStyle(ChatFormatting.ITALIC).withColor(PINK);
        for (ServerPlayer player : level.players()) player.sendSystemMessage(message);
    }

    private ZamaronMeteor() {}
}
