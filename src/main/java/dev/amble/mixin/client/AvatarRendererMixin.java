package dev.amble.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.amble.client.flight.FlightAnimator;
import dev.amble.client.flight.FlightPose;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    private static final float BODY_PIVOT = 0.9F;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void brightestday$extractFlightPose(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        FlightPose pose = entity instanceof Player player ? FlightAnimator.pose(player, partialTicks) : null;
        ((FabricRenderState) state).setData(FlightAnimator.POSE, pose);
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("TAIL"))
    private void brightestday$tiltWhileFlying(AvatarRenderState state, PoseStack poseStack, float bodyRot, float entityScale, CallbackInfo ci) {
        FlightPose pose = ((FabricRenderState) state).getData(FlightAnimator.POSE);
        if (pose == null || state.isFallFlying || state.swimAmount > 0.0F) return;

        poseStack.translate(0.0F, BODY_PIVOT, 0.0F);
        poseStack.rotateDegrees(Axis.ZP, -pose.roll());
        poseStack.rotateDegrees(Axis.XP, pose.bodyAngle());
        poseStack.translate(0.0F, -BODY_PIVOT, 0.0F);
    }
}
