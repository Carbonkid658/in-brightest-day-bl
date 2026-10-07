package dev.amble.mixin;

import dev.amble.core.mounts.ConstructMounts;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin {

    @Inject(method = "openCustomInventoryScreen", at = @At("HEAD"), cancellable = true)
    private void brightestday$constructInventory(Player player, CallbackInfo ci) {
        if (ConstructMounts.isConstruct((AbstractHorse) (Object) this)) ci.cancel();
    }
}
