package dev.amble.client.flight;

import dev.amble.core.ringpowers.impl.FlightRingPower;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public class FlightWindSoundInstance extends AbstractTickableSoundInstance {
    private static final int FADE_IN_TICKS = 20;
    private static final float SPEED_SQUARED_FOR_FULL_VOLUME = 4.0F;
    private static final float PITCH_THRESHOLD = 0.8F;

    private final LocalPlayer player;
    private int time;

    public FlightWindSoundInstance(LocalPlayer player) {
        super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
    }

    @Override
    public void tick() {
        float blend = FlightAnimator.flightBlend(this.player);
        if (this.player.isRemoved() || !FlightRingPower.isFlying(this.player) || (this.time++ > FADE_IN_TICKS && blend < 0.01F)) {
            this.stop();
            return;
        }

        this.x = this.player.getX();
        this.y = this.player.getY();
        this.z = this.player.getZ();

        float speed = FlightAnimator.speed(this.player);
        float loudness = Mth.clamp(speed * speed / SPEED_SQUARED_FOR_FULL_VOLUME, 0.0F, 1.0F);
        this.volume = loudness * blend;
        this.pitch = loudness > PITCH_THRESHOLD ? 1.0F + (loudness - PITCH_THRESHOLD) : 1.0F;
    }
}
