package dev.amble.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.amble.client.render.GlowAura;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin {

    @ModifyExpressionValue(method = "blitAndBlendToTexture", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderPipelines;ENTITY_OUTLINE_BLIT:Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"))
    private RenderPipeline brightestday$additiveOutline(RenderPipeline original) {
        return GlowAura.ADDITIVE_OUTLINE_BLIT;
    }
}
