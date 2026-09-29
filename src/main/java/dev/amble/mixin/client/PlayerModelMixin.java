package dev.amble.mixin.client;

import dev.amble.client.flight.FlightAnimator;
import dev.amble.client.flight.FlightModelPoser;
import dev.amble.client.flight.FlightPose;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void brightestday$flightPose(AvatarRenderState state, CallbackInfo ci) {
        FlightPose pose = ((FabricRenderState) state).getData(FlightAnimator.POSE);
        if (pose == null || state.isFallFlying || state.swimAmount > 0.0F) return;

        FlightModelPoser.apply((PlayerModel) (Object) this, state, pose);
    }
}
