package dev.amble.mixin;

import dev.amble.core.ringpowers.CompactFlyer;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Avatar.class)
public abstract class AvatarMixin {
    @Unique
    private static final EntityDimensions COMPACT = EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F);

    @Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
    private void brightestday$compactWhileFlying(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (this instanceof CompactFlyer flyer && flyer.brightestday$isCompact()) cir.setReturnValue(COMPACT);
    }
}
