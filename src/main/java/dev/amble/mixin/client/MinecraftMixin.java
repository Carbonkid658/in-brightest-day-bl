package dev.amble.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.amble.client.effects.BloodHuntClient;
import dev.amble.client.effects.RingInput;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
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

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void brightestday$glowBloodHuntPrey(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (BloodHuntClient.isPrey(entity)) cir.setReturnValue(true);
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void brightestday$ringOwnsRightClick(CallbackInfo ci) {
        if (this.player != null && ArmedRingPower.isArmed(this.player) && RingInput.wantsUse(this.player)) ci.cancel();
    }

    @WrapOperation(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/EntityHitResult;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult brightestday$noticeEntityInteraction(MultiPlayerGameMode gameMode, Player player, Entity entity, EntityHitResult hit, InteractionHand hand, Operation<InteractionResult> original) {
        InteractionResult result = original.call(gameMode, player, entity, hit, hand);
        if (result instanceof InteractionResult.Success) RingInput.markInteracted();
        return result;
    }

    @WrapOperation(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult brightestday$noticeBlockInteraction(MultiPlayerGameMode gameMode, LocalPlayer player, InteractionHand hand, BlockHitResult hit, Operation<InteractionResult> original) {
        InteractionResult result = original.call(gameMode, player, hand, hit);
        if (result instanceof InteractionResult.Success) RingInput.markInteracted();
        return result;
    }
}
