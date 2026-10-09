package dev.amble.mixin.client;

import dev.amble.client.hud.RingFeed;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudOverlayMixin {
    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void brightestday$routeToRingFeed(Component string, boolean animate, CallbackInfo ci) {
        if (RingFeed.accept(string)) ci.cancel();
    }
}
