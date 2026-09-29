package dev.amble.client.effects;

import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.CorpsColors;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.amble.client.flight.FlightRenderTypes;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import dev.amble.core.ringpowers.impl.LightRingPower;
import dev.amble.core.ringpowers.impl.TractorBeamRingPower;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public final class ArmedPose {
    public static final RenderStateDataKey<Float> AMOUNT = RenderStateDataKey.create(() -> "brightestday:armed_amount");
    public static final RenderStateDataKey<Integer> COLOR = RenderStateDataKey.create(() -> "brightestday:armed_color");
    public static final RenderStateDataKey<Boolean> CHARGED = RenderStateDataKey.create(() -> "brightestday:ring_charged");
    public static final RenderStateDataKey<Float> ACTIVITY = RenderStateDataKey.create(() -> "brightestday:ring_activity");

    public static final float AIM_INWARD = 0.1F;
    private static final float RAISE_SPEED = 0.25F;
    private static final float ACTIVITY_SPEED = 0.3F;

    private static final float FIRST_PERSON_RAISE = 0.12F;
    private static final float FIRST_PERSON_FORWARD = 0.1F;
    private static final float FIRING_INWARD = 0.22F;
    private static final float FIRING_TURN = 18.0F;
    private static final float GLOW_SIZE = 1.5F * VoxelRenderer.PIXEL;
    private static final float GLOW_CORE_WHITENESS = 0.6F;
    private static final float GLOW_CORE_ALPHA = 0.9F;
    private static final float GLOW_HALO_SCALE = 2.2F;
    private static final float GLOW_HALO_ALPHA = 0.45F;
    private static final float GLOW_OUTER_SCALE = 4.0F;
    private static final float GLOW_OUTER_ALPHA = 0.15F;

    private static final Map<Player, float[]> AMOUNTS = new WeakHashMap<>();

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(ArmedPose::tick);
    }

    private static void tick(Minecraft client) {
        if (client.level == null || client.isPaused()) return;

        for (AbstractClientPlayer player : client.level.players()) {
            boolean armed = ArmedRingPower.isArmed(player);
            boolean active = armed && isUsingPower(client, player);
            float[] amount = AMOUNTS.get(player);
            if (amount == null) {
                if (!armed) continue;
                amount = new float[4];
                AMOUNTS.put(player, amount);
            }
            amount[1] = amount[0];
            amount[0] += ((armed ? 1.0F : 0.0F) - amount[0]) * RAISE_SPEED;
            amount[3] = amount[2];
            amount[2] += ((active ? 1.0F : 0.0F) - amount[2]) * ACTIVITY_SPEED;
            if (!armed && amount[0] < 0.001F && amount[2] < 0.001F) AMOUNTS.remove(player);
        }
    }

    private static boolean isUsingPower(Minecraft client, Player player) {
        if (!PowerRingItem.hasCharge(player)) return false;
        if (TractorEffects.isBeaming(player) || BeamEffects.isBeaming(player) || HealBeamEffects.isHealing(player) || TractorBeamRingPower.isActive(player) || LightRingPower.isEmitting(player)) return true;
        return player == client.player && (BlastEffects.firingAmount(1.0F) > 0.01F || ScanEffects.isScanning());
    }

    public static float activity(Player player, float partialTicks) {
        float[] amount = AMOUNTS.get(player);
        return amount == null ? 0.0F : Mth.lerp(partialTicks, amount[3], amount[2]);
    }

    public static float amount(Player player, float partialTicks) {
        float[] amount = AMOUNTS.get(player);
        return amount == null ? 0.0F : Mth.lerp(partialTicks, amount[1], amount[0]);
    }

    public static float amount(Player player) {
        return amount(player, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }

    public static void extract(Avatar entity, AvatarRenderState state, float partialTicks) {
        FabricRenderState data = (FabricRenderState) state;
        if (entity instanceof Player player) {
            data.setData(AMOUNT, amount(player, partialTicks));
            data.setData(COLOR, CorpsColors.of(player));
            data.setData(CHARGED, PowerRingItem.hasCharge(player));
            data.setData(ACTIVITY, activity(player, partialTicks));
        } else {
            data.setData(AMOUNT, 0.0F);
        }
    }

    public static void apply(PlayerModel model, AvatarRenderState state) {
        float amount = ((FabricRenderState) state).getDataOrDefault(AMOUNT, 0.0F);
        if (amount <= 0.001F) return;

        boolean right = state.mainArm == HumanoidArm.RIGHT;
        ModelPart arm = right ? model.rightArm : model.leftArm;
        arm.xRot = Mth.lerp(amount, arm.xRot, model.head.xRot - Mth.HALF_PI);
        arm.yRot = Mth.lerp(amount, arm.yRot, model.head.yRot + (right ? -AIM_INWARD : AIM_INWARD));
        arm.zRot = Mth.lerp(amount, arm.zRot, 0.0F);
    }

    public static void raiseFirstPersonArm(PoseStack poseStack, HumanoidArm arm, AvatarRenderState state) {
        float amount = state.getDataOrDefault(AMOUNT, 0.0F);
        if (amount <= 0.001F || arm != state.mainArm) return;

        poseStack.translate(0.0F, FIRST_PERSON_RAISE * amount, -FIRST_PERSON_FORWARD * amount);

        float firing = BlastEffects.firingAmount(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)) * amount;
        if (firing > 0.001F) {
            float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            poseStack.translate(-side * FIRING_INWARD * firing, 0.0F, 0.0F);
            poseStack.rotateDegrees(Axis.YP, side * FIRING_TURN * firing);
        }
    }

    public static void submitRingGlow(PlayerModel model, AvatarRenderState state, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector collector) {
        FabricRenderState data = state;
        float amount = Math.min(data.getDataOrDefault(AMOUNT, 0.0F), data.getDataOrDefault(ACTIVITY, 0.0F));
        if (amount <= 0.001F || arm != state.mainArm || state.isInvisible || !data.getDataOrDefault(CHARGED, false)) return;

        float pulse = 0.75F + 0.25F * Mth.sin(state.ageInTicks * 0.3F);
        int color = ARGB.opaque(data.getDataOrDefault(COLOR, LanternCorps.GREEN.color()));
        int core = glowColor(VoxelRenderer.toWhite(color, GLOW_CORE_WHITENESS), GLOW_CORE_ALPHA * amount);
        int halo = glowColor(color, GLOW_HALO_ALPHA * amount * pulse);
        int outer = glowColor(color, GLOW_OUTER_ALPHA * amount * pulse);
        float size = GLOW_SIZE * amount * (0.85F + 0.15F * pulse);

        poseStack.pushPose();
        model.translateToHand(state, arm, poseStack);
        poseStack.rotateDegrees(Axis.XP, -90.0F);
        poseStack.rotateDegrees(Axis.YP, 180.0F);
        poseStack.translate((arm == HumanoidArm.LEFT ? -1.0F : 1.0F) / 16.0F, 0.5 / 16.0F, -12.5F / 16.0F);
        collector.submitCustomGeometry(poseStack, FlightRenderTypes.GLOW, (pose, buffer) -> {
            VoxelRenderer.cube(pose, buffer, Vec3.ZERO, size * GLOW_OUTER_SCALE, outer, false);
            VoxelRenderer.cube(pose, buffer, Vec3.ZERO, size * GLOW_HALO_SCALE, halo, false);
            VoxelRenderer.cube(pose, buffer, Vec3.ZERO, size, core, false);
        });
        poseStack.popPose();
    }

    private static int glowColor(int color, float alpha) {
        return ARGB.color(Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255), color);
    }

    private ArmedPose() {}
}
