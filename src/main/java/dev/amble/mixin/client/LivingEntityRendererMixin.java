package dev.amble.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.amble.client.render.ConstructMountRendering;
import dev.amble.client.render.Holograms;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @WrapOperation(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V"))
    private void brightestday$constructMount(SubmitNodeCollector collector, Model<?> model, Object state, PoseStack poseStack, RenderType renderType,
                                             int lightCoords, int overlayCoords, int tintedColor, UvMapping uvMapping, int outlineColor, Operation<Void> original) {
        Integer color = ConstructMountRendering.color(state);
        if (color == null) {
            Integer hologram = Holograms.tint(state);
            if (hologram != null && state instanceof AvatarRenderState avatar) {
                Identifier texture = avatar.skin.body().texturePath();
                original.call(collector, model, state, poseStack, Holograms.type(texture), LightCoordsUtil.FULL_BRIGHT, overlayCoords, hologram, uvMapping, outlineColor);
                return;
            }
            int tint = ConstructMountRendering.bound(state) ? ConstructMountRendering.darken(tintedColor) : tintedColor;
            original.call(collector, model, state, poseStack, renderType, lightCoords, overlayCoords, tint, uvMapping, outlineColor);
            return;
        }
        original.call(collector, model, state, poseStack, ConstructMountRendering.type(ConstructMountRendering.HORSE_TEXTURE),
                LightCoordsUtil.FULL_BRIGHT, overlayCoords, ConstructMountRendering.tint(color), null, outlineColor);
    }

    @Inject(method = "shouldRenderLayers", at = @At("HEAD"), cancellable = true)
    private void brightestday$noConstructLayers(LivingEntityRenderState state, CallbackInfoReturnable<Boolean> cir) {
        if (ConstructMountRendering.color(state) != null) cir.setReturnValue(false);
    }
}
