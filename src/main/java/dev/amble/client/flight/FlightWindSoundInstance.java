package dev.amble.client.flight;

import dev.amble.core.ringpowers.impl.FlightRingPower;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public class FlightWindSoundInstance extends AbstractTickableSoundInstance {
    private final LocalPlayer player;

    public FlightWindSoundInstance(LocalPlayer player) {
        super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
    }

    @Override
    public void tick() {
        if (this.player.isRemoved() || !FlightRingPower.isFlying(this.player)) {
            this.stop();
            return;
        }

        this.x = this.player.getX();
        this.y = this.player.getY();
        this.z = this.player.getZ();

        float intensity = Mth.clamp(FlightAnimator.speed(this.player) / (float) FlightRingPower.BOOST_SPEED, 0.0F, 1.0F);
        this.volume = intensity * 0.8F;
        this.pitch = 0.8F + intensity * 0.6F;
    }
}
