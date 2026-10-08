package dev.amble.mixin;

import net.minecraft.world.entity.decoration.Mannequin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mannequin.class)
public interface MannequinAccessor {
    @Invoker("setImmovable")
    void brightestday$setImmovable(boolean immovable);

    @Invoker("setHideDescription")
    void brightestday$setHideDescription(boolean hide);
}
