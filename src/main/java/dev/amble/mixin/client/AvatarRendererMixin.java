package dev.amble.mixin.client;

import dev.amble.client.effects.ArmedPose;
import dev.amble.client.render.SlottedRingRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void brightestday$extractSlottedRing(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        SlottedRingRenderer.extract(entity, state);
        ArmedPose.extract(entity, state, partialTicks);
    }
}
