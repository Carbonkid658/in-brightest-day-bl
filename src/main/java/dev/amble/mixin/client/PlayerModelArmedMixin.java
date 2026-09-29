package dev.amble.mixin.client;

import dev.amble.client.effects.ArmedPose;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerModel.class, priority = 3000)
public abstract class PlayerModelArmedMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("RETURN"))
    private void brightestday$aimArm(AvatarRenderState state, CallbackInfo ci) {
        ArmedPose.apply((PlayerModel) (Object) this, state);
    }
}
