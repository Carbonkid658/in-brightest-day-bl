package dev.amble.client.effects;

import dev.amble.core.networking.payloads.s2c.BatteriesS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class BatteryEffects {
    private static final float SHELL_HALF = 1.53F;
    private static final float INNER_GLOW = 1.04F;
    private static final float OUTER_GLOW = 1.1F;
    private static final float GLOW_ALPHA = 0.95F;
    private static final float GLASS_ALPHA = 0.7F;
    private static final double RENDER_DISTANCE = 160.0;

    private static List<BatteriesS2CPayload.Entry> batteries = List.of();

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(BatteriesS2CPayload.TYPE, (payload, context) -> batteries = payload.batteries());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> batteries = List.of());
        LevelRenderEvents.COLLECT_SUBMITS.register(BatteryEffects::render);
    }

    private static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (batteries.isEmpty() || client.level == null || client.level.dimension() != Level.OVERWORLD) return;

        Vec3 camera = context.levelState().cameraRenderState.pos;
        float time = client.level.getGameTime() + client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        List<ShieldEffects.Voxel> shells = new ArrayList<>();
        for (BatteriesS2CPayload.Entry battery : batteries) {
            Vec3 center = Vec3.atCenterOf(battery.pos());
            if (center.distanceTo(camera) > RENDER_DISTANCE) continue;
            int color = ARGB.opaque(battery.color());
            float pulse = 0.5F + 0.5F * Mth.sin(time * 0.08F);
            shells.add(new ShieldEffects.Voxel(center, SHELL_HALF + 0.02F * pulse, VoxelRenderer.toWhite(color, 0.5F + 0.3F * pulse)));
        }
        if (shells.isEmpty()) return;
        ShieldEffects.submit(context, camera, shells, INNER_GLOW, GLOW_ALPHA, GLASS_ALPHA);
        ShieldEffects.submitGlow(context, camera, shells, OUTER_GLOW, GLOW_ALPHA * 0.6F);
    }

    private BatteryEffects() {}
}
