package dev.amble.client.effects;

import dev.amble.core.networking.payloads.s2c.CompassS2CPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CompassEffects {
    private static final int FADE_TICKS = 8;
    private static final double REACH = 1.1;
    private static final double DROP = 0.3;
    private static final double DIAL_RADIUS = 0.32;
    private static final int DIAL_VOXELS = 20;
    private static final double NEEDLE_FRONT = 0.3;
    private static final double NEEDLE_BACK = 0.18;
    private static final double NEEDLE_STEP = 0.04;
    private static final float VOXEL_HALF = 0.022F;
    private static final float TIP_HALF = 0.036F;
    private static final float ALPHA = 0.9F;

    private static final class Reading {
        final BlockPos target;
        final int color;
        final int duration;
        int age;

        Reading(BlockPos target, int color, int duration) {
            this.target = target;
            this.color = color;
            this.duration = duration;
        }
    }

    private static final Map<Integer, Reading> READINGS = new HashMap<>();

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(CompassS2CPayload.TYPE, (payload, context) ->
                READINGS.put(payload.playerId(), new Reading(payload.target(), ARGB.opaque(payload.color()), payload.duration())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> READINGS.clear());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.isPaused()) return;
            READINGS.values().removeIf(reading -> ++reading.age > reading.duration);
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(CompassEffects::render);
    }

    private static void render(LevelRenderContext context) {
        if (READINGS.isEmpty()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        Vec3 camera = context.levelState().cameraRenderState.pos;
        float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        for (Map.Entry<Integer, Reading> entry : READINGS.entrySet()) {
            if (!(client.level.getEntity(entry.getKey()) instanceof Player player)) continue;
            Reading reading = entry.getValue();
            float time = reading.age + partialTicks;
            float fade = Mth.clamp(Math.min(time, reading.duration - time) / FADE_TICKS, 0.0F, 1.0F);
            if (fade <= 0.01F) continue;

            Vec3 look = player.getViewVector(partialTicks);
            Vec3 flat = new Vec3(look.x, 0.0, look.z);
            if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(0.0, 0.0, 1.0);
            Vec3 center = player.getEyePosition(partialTicks).add(flat.normalize().scale(REACH)).subtract(0.0, DROP, 0.0);
            Vec3 toward = Vec3.atCenterOf(reading.target).subtract(center);
            Vec3 needle = new Vec3(toward.x, 0.0, toward.z);
            needle = needle.lengthSqr() < 1.0E-4 ? flat.normalize() : needle.normalize();

            List<ShieldEffects.Voxel> voxels = new ArrayList<>();
            for (int i = 0; i < DIAL_VOXELS; i++) {
                float angle = i * Mth.TWO_PI / DIAL_VOXELS + time * 0.02F;
                Vec3 point = center.add(Mth.cos(angle) * DIAL_RADIUS, 0.0, Mth.sin(angle) * DIAL_RADIUS);
                voxels.add(new ShieldEffects.Voxel(point, VOXEL_HALF, VoxelRenderer.toWhite(reading.color, 0.1F)));
            }
            for (double d = -NEEDLE_BACK; d <= NEEDLE_FRONT; d += NEEDLE_STEP) {
                int tint = d > 0 ? VoxelRenderer.toWhite(reading.color, 0.4F) : reading.color;
                voxels.add(new ShieldEffects.Voxel(center.add(needle.scale(d)), VOXEL_HALF, tint));
            }
            voxels.add(new ShieldEffects.Voxel(center.add(needle.scale(NEEDLE_FRONT + NEEDLE_STEP)), TIP_HALF, VoxelRenderer.toWhite(reading.color, 0.6F)));
            ShieldEffects.submit(context, camera, voxels, ALPHA * fade);
        }
    }

    private CompassEffects() {}
}
