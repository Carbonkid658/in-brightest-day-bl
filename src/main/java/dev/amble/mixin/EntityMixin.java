package dev.amble.mixin;

import dev.amble.core.ringpowers.RingBenefits;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "fireImmune", at = @At("HEAD"), cancellable = true)
    private void brightestday$ringFireImmunity(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player && RingBenefits.isActive(player)) cir.setReturnValue(true);
    }
}
