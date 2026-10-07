package dev.amble.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.amble.client.render.ConstructMountRendering;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractBoatRenderer.class)
public abstract class AbstractBoatRendererMixin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @WrapOperation(method = "submit(Lnet/minecraft/client/renderer/entity/state/BoatRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/Identifier;III)V"))
    private void brightestday$constructBoat(SubmitNodeCollector collector, Model model, Object state, PoseStack poseStack, Identifier texture,
                                            int lightCoords, int overlayCoords, int outlineColor, Operation<Void> original) {
        Integer color = ConstructMountRendering.color(state);
        if (color == null) {
            original.call(collector, model, state, poseStack, texture, lightCoords, overlayCoords, outlineColor);
            return;
        }
        collector.submitModel(model, state, poseStack, ConstructMountRendering.type(texture), LightCoordsUtil.FULL_BRIGHT, overlayCoords,
                ConstructMountRendering.tint(color), null, outlineColor);
    }
}
