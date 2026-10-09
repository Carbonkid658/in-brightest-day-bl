package dev.amble.mixin.client;

import dev.amble.client.effects.LanternChargeAnimations;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityTurnMixin {
    @Unique
    private static final float RITUAL_LOOK_LIMIT = 90.0F;

    @Inject(method = "turn", at = @At("TAIL"))
    private void brightestday$limitRitualLook(double xo, double yo, CallbackInfo ci) {
        if (!((Object) this instanceof LocalPlayer player)) return;
        Float yaw = LanternChargeAnimations.ritualYaw(player);
        if (yaw == null) return;

        float offset = Mth.wrapDegrees(player.getYRot() - yaw);
        float clamped = Mth.clamp(offset, -RITUAL_LOOK_LIMIT, RITUAL_LOOK_LIMIT);
        if (clamped == offset) return;
        player.setYRot(yaw + clamped);
        player.yRotO = player.getYRot();
    }
}
