package dev.amble.mixin;

import dev.amble.core.progression.EmotionSources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
public abstract class AnimalMixin {

    @Inject(method = "finalizeSpawnChildFromBreeding", at = @At("HEAD"))
    private void brightestday$loveBreed(ServerLevel level, Animal partner, AgeableMob offspring, CallbackInfo ci) {
        ServerPlayer cause = ((Animal) (Object) this).getLoveCause();
        if (cause == null) cause = partner.getLoveCause();
        if (cause != null) EmotionSources.onBreed(cause);
    }
}
