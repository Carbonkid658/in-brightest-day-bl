package dev.amble.mixin.client;

import dev.amble.client.compat.ReplaySnapshot;
import net.fabricmc.fabric.impl.attachment.sync.AttachmentChange;
import net.fabricmc.fabric.impl.attachment.sync.AttachmentTargetInfo;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AttachmentChange.class)
public abstract class AttachmentChangeMixin {

    @Inject(method = "tryApply", at = @At("HEAD"), cancellable = true, require = 0)
    private void brightestday$deferMissingReplayEntity(Level level, CallbackInfo ci) {
        AttachmentChange change = (AttachmentChange) (Object) this;
        if (change.targetInfo() instanceof AttachmentTargetInfo.EntityTarget(int networkId)
                && level.getEntity(networkId) == null
                && ReplaySnapshot.defer(networkId, change.type(), change.value())) {
            ci.cancel();
        }
    }
}
