package dev.amble.client.effects.attacks.projectile;

import dev.amble.client.effects.ShieldEffects;
import dev.amble.client.effects.VoxelRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ProjectileBursts {
    private static final double DRAG = 0.82;
    private static final float HEAT_TICKS = 3.0F;

    private static final List<Spark> SPARKS = new ArrayList<>();
    private static @Nullable ClientLevel trackedLevel;

    private static final class Spark {
        Vec3 previous;
        Vec3 position;
        Vec3 velocity;
        final int color;
        final float half;
        final int lifetime;
        int delay;
        int age;

        Spark(Vec3 position, Vec3 velocity, int color, float half, int lifetime, int delay) {
            this.previous = position;
            this.position = position;
            this.velocity = velocity;
            this.color = color;
            this.half = half;
            this.lifetime = lifetime;
            this.delay = delay;
        }
    }

    public static void init() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SPARKS.clear());
        ClientTickEvents.END_CLIENT_TICK.register(ProjectileBursts::tick);
        LevelRenderEvents.COLLECT_SUBMITS.register(ProjectileBursts::render);
    }

    public static void spawn(Vec3 at, int color, int count, float speed, float size, int lifetime) {
        spawn(at, color, count, speed, size, lifetime, 0);
    }

    public static void spawn(Vec3 at, int color, int count, float speed, float size, int lifetime, int delay) {
        RandomSource random = RandomSource.create();
        for (int i = 0; i < count; i++) {
            Vec3 direction = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
            Vec3 velocity = direction.scale(speed * (0.35F + random.nextFloat() * 0.65F));
            float half = size * (0.6F + random.nextFloat() * 0.4F) * 0.5F;
            SPARKS.add(new Spark(at, velocity, color, half, lifetime + random.nextInt(Math.max(lifetime / 3, 1)), delay));
        }
    }

    private static void tick(Minecraft client) {
        if (client.level != trackedLevel) {
            SPARKS.clear();
            trackedLevel = client.level;
        }
        if (client.level == null || client.isPaused()) return;

        Iterator<Spark> iterator = SPARKS.iterator();
        while (iterator.hasNext()) {
            Spark spark = iterator.next();
            if (spark.delay > 0) {
                spark.delay--;
                continue;
            }
            spark.previous = spark.position;
            spark.position = spark.position.add(spark.velocity);
            spark.velocity = spark.velocity.scale(DRAG);
            if (++spark.age > spark.lifetime) iterator.remove();
        }
    }

    private static void render(LevelRenderContext context) {
        if (SPARKS.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 camera = context.levelState().cameraRenderState.pos;

        List<ShieldEffects.Voxel> voxels = new ArrayList<>(SPARKS.size());
        for (Spark spark : SPARKS) {
            if (spark.delay > 0) continue;
            float time = spark.age + partialTicks;
            float life = 1.0F - Mth.clamp(time / spark.lifetime, 0.0F, 1.0F);
            float half = spark.half * life;
            if (half < VoxelRenderer.PIXEL * 0.25F) continue;

            int tint = VoxelRenderer.toWhite(spark.color, 1.0F - Mth.clamp(time / HEAT_TICKS, 0.0F, 1.0F) * 0.7F);
            voxels.add(new ShieldEffects.Voxel(VoxelRenderer.snap(spark.previous.lerp(spark.position, partialTicks)), VoxelRenderer.snapSize(half), tint));
        }
        if (!voxels.isEmpty()) ShieldEffects.submit(context, camera, voxels, 0.9F);
    }

    private ProjectileBursts() {}
}
