package dev.amble.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.amble.client.effects.ArmedPose;
import dev.amble.client.render.SlottedRingRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsAndItemsRendererMixin {

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "renderPlayerArm", at = @At("HEAD"))
    private void brightestday$raiseArmedArm(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, float inverseArmHeight, float attackValue, HumanoidArm arm, PlayerRenderState playerState, CallbackInfo ci) {
        if (playerState.avatarRenderState != null) ArmedPose.raiseFirstPersonArm(poseStack, arm, playerState.avatarRenderState);
    }

    @Inject(method = "renderPlayerHand", at = @At("TAIL"))
    private void brightestday$renderSlottedRing(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, HumanoidArm arm, PlayerRenderState playerState, CallbackInfo ci) {
        AvatarRenderState state = playerState.avatarRenderState;
        if (state == null) return;

        AvatarRenderer<?> renderer = this.minecraft.getEntityRenderDispatcher().getRenderer(state);
        SlottedRingRenderer.submit(renderer.getModel(), state, arm, poseStack, submitNodeCollector, lightCoords, 0);
        ArmedPose.submitRingGlow(renderer.getModel(), state, arm, poseStack, submitNodeCollector);
    }
}
