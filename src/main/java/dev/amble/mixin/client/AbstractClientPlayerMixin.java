package dev.amble.mixin.client;

import dev.amble.client.flight.FlightAnimator;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void brightestday$flightFov(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
        float boost = FlightAnimator.fovBoost((AbstractClientPlayer) (Object) this, firstPerson);
        if (boost > 0.0F && cir.getReturnValue() > 0.5F) {
            cir.setReturnValue(cir.getReturnValue() * Mth.lerp(effectScale, 1.0F, 1.0F + boost));
        }
    }
}
