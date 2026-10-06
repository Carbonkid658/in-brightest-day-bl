package dev.amble.mixin;

import dev.amble.core.progression.EmotionSources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ZombieVillager.class)
public abstract class ZombieVillagerMixin {
    @Shadow
    private @Nullable UUID conversionStarter;

    @Inject(method = "finishConversion", at = @At("HEAD"))
    private void brightestday$hopeCure(ServerLevel level, CallbackInfo ci) {
        if (this.conversionStarter != null && level.getPlayerByUUID(this.conversionStarter) instanceof ServerPlayer player) EmotionSources.onCure(player);
    }
}
