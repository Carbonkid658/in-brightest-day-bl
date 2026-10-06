package dev.amble.mixin;

import dev.amble.core.progression.EmotionSources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin {

    @Inject(method = "notifyTrade", at = @At("HEAD"))
    private void brightestday$hopeTrade(MerchantOffer offer, CallbackInfo ci) {
        if (((AbstractVillager) (Object) this).getTradingPlayer() instanceof ServerPlayer player) EmotionSources.onTrade(player);
    }
}
