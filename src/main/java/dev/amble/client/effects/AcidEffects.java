package dev.amble.client.effects;

import dev.amble.core.acid.AcidStream;
import dev.amble.core.networking.payloads.s2c.AcidS2CPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class AcidEffects {
    private static final int DROPS_PER_TICK = 5;
    private static final int MAX_AGE = 30;
    private static final double JITTER = 0.05;
    private static final float DROP_SIZE = 1.5F * VoxelRenderer.PIXEL;
    private static final int[] PALETTE = {0xFFE0321A, 0xFFFF5A1F, 0xFFFF8A2A, 0xFFB81414};

    private static final Set<Integer> SPEWING = new HashSet<>();
    private static final List<Drop> DROPS = new ArrayList<>();
    private static @Nullable ClientLevel trackedLevel;

    private static final class Drop {
        Vec3 previous;
        Vec3 position;
        Vec3 velocity;
        final int color;
        int age;

        Drop(Vec3 position, Vec3 velocity, int color) {
            this.previous = position;
            this.position = position;
            this.velocity = velocity;
            this.color = color;
        }
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(AcidS2CPayload.TYPE, (payload, context) -> {
            if (payload.active()) {
                SPEWING.add(payload.playerId());
            } else {
                SPEWING.remove(payload.playerId());
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SPEWING.clear();
            DROPS.clear();
        });
        ClientTickEvents.END_CLIENT_TICK.register(AcidEffects::tick);
        LevelRenderEvents.COLLECT_SUBMITS.register(AcidEffects::render);
    }

    private static void tick(Minecraft client) {
        if (client.level != trackedLevel) {
            SPEWING.clear();
            DROPS.clear();
            trackedLevel = client.level;
        }
        if (client.level == null || client.isPaused()) return;

        RandomSource random = client.level.getRandom();
        for (int id : SPEWING) {
            Entity entity = client.level.getEntity(id);
            if (entity == null) continue;

            Vec3 mouth = AcidStream.mouth(entity, 1.0F);
            Vec3 velocity = AcidStream.velocity(entity.getViewVector(1.0F));
            for (int i = 0; i < DROPS_PER_TICK; i++) {
                Vec3 jitter = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).scale(JITTER);
                DROPS.add(new Drop(mouth, velocity.add(jitter), PALETTE[random.nextInt(PALETTE.length)]));
            }
        }

        Iterator<Drop> iterator = DROPS.iterator();
        while (iterator.hasNext()) {
            Drop drop = iterator.next();
            drop.previous = drop.position;
            drop.position = drop.position.add(drop.velocity);
            drop.velocity = drop.velocity.add(0.0, -AcidStream.GRAVITY, 0.0);
            BlockPos pos = BlockPos.containing(drop.position);
            if (++drop.age > MAX_AGE || !client.level.getBlockState(pos).getCollisionShape(client.level, pos).isEmpty()) iterator.remove();
        }
    }

    private static void render(LevelRenderContext context) {
        if (DROPS.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 camera = context.levelState().cameraRenderState.pos;

        List<ShieldEffects.Voxel> voxels = new ArrayList<>(DROPS.size());
        for (Drop drop : DROPS) {
            float life = 1.0F - Mth.clamp((drop.age + partialTicks) / MAX_AGE, 0.0F, 1.0F);
            float half = VoxelRenderer.snapSize(DROP_SIZE * (0.5F + 0.5F * life));
            voxels.add(new ShieldEffects.Voxel(VoxelRenderer.snap(drop.previous.lerp(drop.position, partialTicks)), half, drop.color));
        }
        ShieldEffects.submit(context, camera, voxels, 0.9F);
    }

    private AcidEffects() {}
}
