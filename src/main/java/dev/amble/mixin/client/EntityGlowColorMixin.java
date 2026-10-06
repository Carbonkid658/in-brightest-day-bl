package dev.amble.mixin.client;

import dev.amble.client.effects.BloodHuntClient;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityGlowColorMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void brightestday$bloodHuntColor(CallbackInfoReturnable<Integer> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity.level().isClientSide() && BloodHuntClient.isPrey(entity)) cir.setReturnValue(BloodHuntClient.GLOW_COLOR);
    }
}
