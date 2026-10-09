package dev.amble.client.compat.jei;

import dev.amble.core.forge.ForgeRecipe;
import dev.amble.core.forge.ForgeRecipes;
import dev.amble.core.progression.Emotion;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ForgeDisplay(ForgeRecipe recipe, Emotion emotion, boolean lava, List<ItemStack> outputs) {

    public static ForgeDisplay of(ForgeRecipe recipe, Emotion emotion, boolean lava) {
        return new ForgeDisplay(recipe, emotion, lava, recipe.outputs().apply(null));
    }

    public boolean fuse() {
        return this.recipe.key().equals(ForgeRecipes.FUSE);
    }
}
