package dev.amble.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.amble.client.effects.LanternChargeAnimations;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.amble.client.render.ConstructToolRendering;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
public abstract class ItemStackLayerRenderStateMixin {

    @Shadow @Final ItemStackRenderState this$0;
    @Shadow private ItemQuads quads;
    @Shadow private ItemStackRenderState.FoilType foilType;
    @Shadow private @Nullable IntList tintLayers;
    @Shadow private @Nullable SpecialModelRenderer<Object> specialRenderer;

    @Shadow
    protected abstract void applyTransform(PoseStack.Pose localPose);

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void brightestday$submitConstruct(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, int outlineColor, CallbackInfo ci) {
        if (this.specialRenderer != null || !ConstructToolRendering.isConstruct(this.this$0)) return;

        poseStack.pushPose();
        this.applyTransform(poseStack.last());
        ConstructToolRendering.submit(poseStack, collector, overlayCoords, this.quads, this.tintLayers, this.foilType != ItemStackRenderState.FoilType.NONE);
        poseStack.popPose();
        ci.cancel();
    }

    @WrapWithCondition(method = "applyTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/cuboid/ItemTransform;apply(ZLcom/mojang/blaze3d/vertex/PoseStack$Pose;)V"))
    private boolean brightestday$bareChargeLantern(ItemTransform transform, boolean leftHand, PoseStack.Pose pose) {
        return !LanternChargeAnimations.bareTransform();
    }
}
