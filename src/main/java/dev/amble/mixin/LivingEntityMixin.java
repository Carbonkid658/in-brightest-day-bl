package dev.amble.mixin;

import dev.amble.core.ringpowers.RingBenefits;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "canBreatheUnderwater", at = @At("HEAD"), cancellable = true)
    private void brightestday$ringWaterBreathing(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player && RingBenefits.isActive(player)) cir.setReturnValue(true);
    }
}
