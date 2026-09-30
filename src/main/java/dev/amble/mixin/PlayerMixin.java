package dev.amble.mixin;

import dev.amble.core.ringpowers.RingBenefits;
import dev.amble.core.ringpowers.impl.FlightRingPower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {

    protected PlayerMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Inject(method = "causeFoodExhaustion", at = @At("HEAD"), cancellable = true)
    private void brightestday$ringSustains(float amount, CallbackInfo ci) {
        if (RingBenefits.isActive((Player) (Object) this)) ci.cancel();
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void brightestday$ringFlight(Vec3 input, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self.isLocalPlayer() && FlightRingPower.isFlying(self)) {
            FlightRingPower.travel(self, input, this.jumping);
            ci.cancel();
        }
    }

    @Inject(method = "getDesiredPose", at = @At("HEAD"), cancellable = true)
    private void brightestday$standWhileFlying(CallbackInfoReturnable<Pose> cir) {
        if (FlightRingPower.isFlying((Player) (Object) this)) {
            cir.setReturnValue(Pose.STANDING);
        }
    }

    @Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
    private void brightestday$noSwimmingWhileFlying(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (FlightRingPower.isFlying(self)) {
            self.setSwimming(false);
            ci.cancel();
        }
    }
}
