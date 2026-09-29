package dev.amble.client.effects;

import dev.amble.core.ringpowers.CorpsColors;
import dev.amble.core.blocks.LanternBlock;
import dev.amble.core.networking.payloads.c2s.CycleConstructC2SPayload;
import dev.amble.core.ringpowers.constructs.AreaShieldConstruct;
import dev.amble.core.ringpowers.constructs.ConstructRingPower;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ConstructClient {
    private static final float PREVIEW_ALPHA = 0.3F;

    private static int radius = AreaShieldConstruct.DEFAULT_RADIUS;

    public static void init() {
        LevelRenderEvents.COLLECT_SUBMITS.register(ConstructClient::renderPreview);
    }

    public static int radius() {
        return radius;
    }

    public static void cycle() {
        ClientPlayNetworking.send(CycleConstructC2SPayload.INSTANCE);
    }

    public static boolean isLookingAtLantern() {
        Minecraft client = Minecraft.getInstance();
        return client.level != null
                && client.hitResult instanceof BlockHitResult blockHit
                && blockHit.getType() == HitResult.Type.BLOCK
                && client.level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof LanternBlock;
    }

    public static boolean onScroll(int wheel) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !isSizingAreaShield(player)) return false;

        int resized = AreaShieldConstruct.clampRadius(radius + Integer.signum(wheel));
        if (resized != radius) {
            radius = resized;
            Minecraft.getInstance().gui.hud.setOverlayMessage(Component.translatable("message.brightestday.shield_radius", radius), false);
        }
        return true;
    }

    private static boolean isSizingAreaShield(LocalPlayer player) {
        return player.getMainHandItem().isEmpty()
                && ArmedRingPower.isArmed(player)
                && ArmedRingPower.selectedConstruct(player).map(ConstructRingPower::usesRadius).orElse(false);
    }

    private static void renderPreview(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || !isSizingAreaShield(player) || isLookingAtLantern()) return;

        float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 target = player.pick(AreaShieldConstruct.RANGE, partialTicks, false).getLocation();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        int color = ARGB.opaque(CorpsColors.of(player));
        float time = player.tickCount + partialTicks;

        ShieldEffects.submit(context, camera, ShieldEffects.sphere(target, radius, radius * 2.0F, time, color), PREVIEW_ALPHA);
    }

    private ConstructClient() {}
}
