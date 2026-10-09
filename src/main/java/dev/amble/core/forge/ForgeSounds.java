package dev.amble.core.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/** Forge sound effects: every forge plays its own sound; the Black Anvil layers a Deep Dark sound on top of it. */
final class ForgeSounds {
    static void play(Level level, BlockPos pos, SoundEvent normal, SoundEvent sculk, float volume, float pitch) {
        level.playSound(null, pos, normal, SoundSource.BLOCKS, volume, pitch);
        if (level.getBlockState(pos).getBlock() instanceof BlackAnvilBlock) {
            level.playSound(null, pos, sculk, SoundSource.BLOCKS, volume, pitch);
        }
    }

    private ForgeSounds() {}
}
