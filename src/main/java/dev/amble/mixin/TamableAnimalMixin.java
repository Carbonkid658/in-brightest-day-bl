package dev.amble.mixin;

import dev.amble.core.progression.EmotionSources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TamableAnimal.class)
public abstract class TamableAnimalMixin {

    @Inject(method = "tame", at = @At("HEAD"))
    private void brightestday$loveTame(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer server) EmotionSources.onTame(server);
    }
}
