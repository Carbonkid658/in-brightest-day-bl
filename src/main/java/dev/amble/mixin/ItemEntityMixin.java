package dev.amble.mixin;

import dev.amble.core.progression.EmotionSources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(method = "playerTouch", at = @At("RETURN"))
    private void brightestday$gift(Player player, CallbackInfo ci) {
        ItemEntity item = (ItemEntity) (Object) this;
        if (!item.isRemoved() || !(item.getOwner() instanceof ServerPlayer giver) || giver == player) return;
        EmotionSources.onGift(giver, player);
    }
}
