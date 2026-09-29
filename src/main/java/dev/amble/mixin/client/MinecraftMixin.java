package dev.amble.mixin.client;

import dev.amble.client.effects.BlastEffects;
import dev.amble.client.effects.TractorEffects;
import dev.amble.core.ringpowers.impl.FlightRingPower;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow public @Nullable LocalPlayer player;

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void brightestday$noPunchWhileFlying(CallbackInfoReturnable<Boolean> cir) {
        if (this.player != null && (FlightRingPower.isFlying(this.player) || TractorEffects.isTractorMode(this.player))) cir.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void brightestday$noMiningWhileFlying(boolean down, CallbackInfo ci) {
        if (this.player != null && (FlightRingPower.isFlying(this.player) || TractorEffects.isTractorMode(this.player))) ci.cancel();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void brightestday$fireBlast(CallbackInfo ci) {
        if (this.player != null && BlastEffects.wantsToCharge(this.player)) ci.cancel();
    }
}
