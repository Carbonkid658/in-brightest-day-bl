package dev.amble.mixin;

import dev.amble.core.mounts.ConstructMounts;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin {

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void brightestday$constructMount(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        Mob self = (Mob) (Object) this;
        if (!ConstructMounts.isConstruct(self)) return;
        if (!self.level().isClientSide() && ConstructMounts.ownedBy(self, player) && !self.isVehicle()) player.startRiding(self);
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
