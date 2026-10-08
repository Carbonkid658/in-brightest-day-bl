package dev.amble.client.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.amble.client.flight.FlightRenderTypes;
import dev.amble.client.render.BatteryTextures;
import dev.amble.client.render.models.CentralPowerBatteryModel;
import dev.amble.core.networking.payloads.s2c.BatteriesS2CPayload;
import dev.amble.core.ringpowers.LanternCorps;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class BatteryEffects {
    private static final int LIGHT_SAMPLE_OFFSET = 4;
    private static final float EMISSION_WHITENESS = 0.25F;
    private static final float PANEL_DEPTH = 2.9F;
    private static final float PANEL_RADIUS = 1.15F;
    private static final int SIDES = 8;
    private static final float HEAD_ON = 0.92F;
    private static final float HEAD_ON_FADE = 0.35F;
    private static final float RAY_LENGTH = 6.5F;
    private static final float PANE_DEPTH = 2.38F;
    private static final float PANE_RADIUS = 1.25F;
    private static final float PULSE_SPEED = 0.09F;
    private static final float PANE_WHITE_MIN = 0.15F;
    private static final float PANE_WHITE_MAX = 0.75F;
    private static final float[][] BEAMS = {
            {0.55F, 1.25F, 0.55F, 0.8F},
            {0.9F, 1.7F, 0.32F, 0.45F},
            {1.2F, 2.2F, 0.16F, 0.15F}
    };
    private static final float[][] PANES = {
            {1.0F, 0.4F, 0.0F},
            {0.55F, 0.55F, 0.55F}
    };
    private static final double RENDER_DISTANCE = 160.0;

    private static List<BatteriesS2CPayload.Entry> batteries = List.of();
    private static @Nullable CentralPowerBatteryModel model;

    public static boolean isBattery(BlockPos pos) {
        for (BatteriesS2CPayload.Entry battery : batteries) {
            BlockPos core = battery.pos();
            if (Math.abs(pos.getX() - core.getX()) <= 1 && Math.abs(pos.getY() - core.getY()) <= 1 && Math.abs(pos.getZ() - core.getZ()) <= 1) return true;
        }
        return false;
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(BatteriesS2CPayload.TYPE, (payload, context) -> batteries = payload.batteries());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> batteries = List.of());
        LevelRenderEvents.COLLECT_SUBMITS.register(BatteryEffects::render);
    }

    private static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (batteries.isEmpty() || client.level == null || client.level.dimension() != Level.OVERWORLD || !BatteryTextures.ready()) return;
        if (model == null) model = new CentralPowerBatteryModel();

        Vec3 camera = context.levelState().cameraRenderState.pos;
        float time = client.level.getGameTime() + client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        PoseStack poseStack = context.poseStack();
        for (BatteriesS2CPayload.Entry battery : batteries) {
            Vec3 center = Vec3.atCenterOf(battery.pos());
            if (center.distanceTo(camera) > RENDER_DISTANCE) continue;
            int color = ARGB.opaque(battery.color());
            BlockPos sample = battery.pos().north(LIGHT_SAMPLE_OFFSET);
            int light = LightCoordsUtil.pack(client.level.getBrightness(LightLayer.BLOCK, sample), client.level.getBrightness(LightLayer.SKY, sample));
            float pulse = 0.5F + 0.5F * Mth.sin(time * 0.08F);

            boolean green = battery.color() == LanternCorps.GREEN.color();
            Identifier base = green ? BatteryTextures.BASE_SOURCE : BatteryTextures.BASE;
            Identifier emission = green ? BatteryTextures.EMISSION_SOURCE : BatteryTextures.EMISSION;
            int tint = green ? -1 : color;

            poseStack.pushPose();
            poseStack.translate(center.x - camera.x, center.y - camera.y, center.z - camera.z);
            poseStack.pushPose();
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(0.0F, CentralPowerBatteryModel.CORE_Y / 16.0F, 0.0F);
            context.submitNodeCollector().submitModelPart(model.body(), poseStack, RenderTypes.entityTranslucent(base), light, OverlayTexture.NO_OVERLAY, null, tint);
            if (battery.active()) {
                int glow = green ? ARGB.white(0.75F + 0.25F * pulse) : VoxelRenderer.toWhite(color, EMISSION_WHITENESS * pulse);
                context.submitNodeCollector().submitModelPart(model.body(), poseStack, RenderTypes.eyes(emission), LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null, glow);
            }
            poseStack.popPose();
            if (battery.active()) rays(context, poseStack, color, time, camera.subtract(center));
            poseStack.popPose();
        }
    }

    private static void rays(LevelRenderContext context, PoseStack poseStack, int color, float time, Vec3 eye) {
        float pulse = 0.5F + 0.5F * Mth.sin(time * PULSE_SPEED);
        float flicker = 0.9F + 0.1F * Mth.sin(time * 0.37F);
        int paneColor = ARGB.srgbLerp(PANE_WHITE_MIN + (PANE_WHITE_MAX - PANE_WHITE_MIN) * pulse, color, 0xFFFFFFFF);
        context.submitNodeCollector().submitCustomGeometry(poseStack, FlightRenderTypes.GLOW, (pose, buffer) -> {
            for (int side = -1; side <= 1; side += 2) {
                for (float[] layer : PANES) {
                    int tint = VoxelRenderer.toWhite(paneColor, layer[2]);
                    pane(pose, buffer, side * PANE_DEPTH, PANE_RADIUS * layer[0], ARGB.color(Math.round(255 * layer[1] * (0.55F + 0.45F * pulse)), tint));
                }
                Vec3 from = new Vec3(0.0, 0.0, side * PANEL_DEPTH);
                Vec3 to = new Vec3(0.0, 0.0, side * (PANEL_DEPTH + RAY_LENGTH));
                float facing = Math.abs((float) from.lerp(to, 0.5).subtract(eye).normalize().z);
                float visibility = Mth.clamp((HEAD_ON - facing) / HEAD_ON_FADE, 0.0F, 1.0F);
                if (visibility <= 0.01F) continue;
                for (float[] layer : BEAMS) {
                    float strength = layer[2] * flicker * (0.75F + 0.25F * pulse) * visibility;
                    frustum(pose, buffer, from, to, PANEL_RADIUS * layer[0], PANEL_RADIUS * layer[1], VoxelRenderer.toWhite(color, layer[3]), strength);
                }
            }
        });
    }

    private static void pane(PoseStack.Pose pose, VertexConsumer buffer, float z, float radius, int color) {
        int edge = ARGB.color(0, color);
        for (int i = 0; i < SIDES; i++) {
            float a0 = (i + 0.5F) * Mth.TWO_PI / SIDES;
            float a1 = (i + 1.5F) * Mth.TWO_PI / SIDES;
            buffer.addVertex(pose, 0.0F, 0.0F, z).setColor(color);
            buffer.addVertex(pose, 0.0F, 0.0F, z).setColor(color);
            buffer.addVertex(pose, Mth.cos(a0) * radius, Mth.sin(a0) * radius, z).setColor(edge);
            buffer.addVertex(pose, Mth.cos(a1) * radius, Mth.sin(a1) * radius, z).setColor(edge);
        }
    }

    private static void frustum(PoseStack.Pose pose, VertexConsumer buffer, Vec3 from, Vec3 to, float nearRadius, float farRadius, int color, float strength) {
        int near = ARGB.color(Math.round(255 * Mth.clamp(strength, 0.0F, 1.0F)), color);
        int far = ARGB.color(0, color);
        for (int i = 0; i < SIDES; i++) {
            float a0 = (i + 0.5F) * Mth.TWO_PI / SIDES;
            float a1 = (i + 1.5F) * Mth.TWO_PI / SIDES;
            buffer.addVertex(pose, Mth.cos(a0) * nearRadius, Mth.sin(a0) * nearRadius, (float) from.z).setColor(near);
            buffer.addVertex(pose, Mth.cos(a1) * nearRadius, Mth.sin(a1) * nearRadius, (float) from.z).setColor(near);
            buffer.addVertex(pose, Mth.cos(a1) * farRadius, Mth.sin(a1) * farRadius, (float) to.z).setColor(far);
            buffer.addVertex(pose, Mth.cos(a0) * farRadius, Mth.sin(a0) * farRadius, (float) to.z).setColor(far);
        }
    }

    private BatteryEffects() {}
}
