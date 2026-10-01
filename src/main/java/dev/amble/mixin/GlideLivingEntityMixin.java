package dev.amble.mixin;

import dev.amble.core.glide.GlideManager;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class GlideLivingEntityMixin extends Entity {
    @Shadow
    protected int fallFlyTicks;

    protected GlideLivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Shadow
    protected abstract boolean canGlide();

    @Inject(method = "canGlide", at = @At("HEAD"), cancellable = true)
    private void brightestday$glideConstruct(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!GlideManager.grantsGlide(self)) return;
        if (!self.onGround() && !self.isPassenger() && !self.hasEffect(MobEffects.LEVITATION)) cir.setReturnValue(true);
    }

    @Inject(method = "updateFallFlying", at = @At("HEAD"), cancellable = true)
    private void brightestday$glideWithoutGlider(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!GlideManager.grantsGlide(self) || this.brightestday$hasGlider(self)) return;

        ci.cancel();
        self.checkFallDistanceAccumulation();
        if (self.level().isClientSide()) return;
        if (!this.canGlide()) {
            this.setSharedFlag(7, false);
            return;
        }
        if ((this.fallFlyTicks + 1) % 10 == 0) self.gameEvent(GameEvent.ELYTRA_GLIDE);
    }

    @Unique
    private boolean brightestday$hasGlider(LivingEntity self) {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (LivingEntity.canGlideUsing(self.getItemBySlot(slot), slot)) return true;
        }
        return false;
    }
}
