package dev.amble.core.comms;

import java.util.concurrent.ThreadLocalRandom;

public final class VoiceFilter {
    private static final float SAMPLE_RATE = 48000.0F;
    private static final float BUTTERWORTH_Q = 0.7071F;

    private final Biquad[] stages;
    private final float gain;
    private final float drive;
    private final int crushBits;
    private final float hiss;

    private VoiceFilter(Biquad[] stages, float gain, float drive, int crushBits, float hiss) {
        this.stages = stages;
        this.gain = gain;
        this.drive = drive;
        this.crushBits = crushBits;
        this.hiss = hiss;
    }

    public static VoiceFilter radio() {
        return new VoiceFilter(new Biquad[]{
                Biquad.highPass(320.0F, BUTTERWORTH_Q),
                Biquad.highPass(320.0F, BUTTERWORTH_Q),
                Biquad.lowPass(3000.0F, BUTTERWORTH_Q),
                Biquad.lowPass(3000.0F, BUTTERWORTH_Q),
                Biquad.peak(1400.0F, 1.2F, 4.0F)
        }, 1.1F, 3.0F, 10, 0.006F);
    }

    public static VoiceFilter megaphone(float gain, float drive) {
        return new VoiceFilter(new Biquad[]{
                Biquad.highPass(480.0F, BUTTERWORTH_Q),
                Biquad.highPass(480.0F, BUTTERWORTH_Q),
                Biquad.lowPass(3800.0F, BUTTERWORTH_Q),
                Biquad.peak(1800.0F, 1.4F, 7.0F)
        }, gain, drive, 0, 0.0F);
    }

    public short[] process(short[] input) {
        short[] output = new short[input.length];
        float step = this.crushBits > 0 ? 2.0F / (1 << this.crushBits) : 0.0F;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < input.length; i++) {
            float sample = input[i] / 32768.0F;
            for (Biquad stage : this.stages) sample = stage.process(sample);
            sample = (float) Math.tanh(sample * this.drive) / this.drive * this.gain;
            if (step > 0.0F) sample = Math.round(sample / step) * step;
            if (this.hiss > 0.0F) sample += (random.nextFloat() * 2.0F - 1.0F) * this.hiss;
            output[i] = (short) Math.clamp(Math.round(sample * 32767.0F), Short.MIN_VALUE, Short.MAX_VALUE);
        }
        return output;
    }

    public void reset() {
        for (Biquad stage : this.stages) stage.reset();
    }

    private static final class Biquad {
        private final float b0;
        private final float b1;
        private final float b2;
        private final float a1;
        private final float a2;
        private float x1;
        private float x2;
        private float y1;
        private float y2;

        private Biquad(double b0, double b1, double b2, double a0, double a1, double a2) {
            this.b0 = (float) (b0 / a0);
            this.b1 = (float) (b1 / a0);
            this.b2 = (float) (b2 / a0);
            this.a1 = (float) (a1 / a0);
            this.a2 = (float) (a2 / a0);
        }

        static Biquad lowPass(float frequency, float q) {
            double w = 2.0 * Math.PI * frequency / SAMPLE_RATE;
            double alpha = Math.sin(w) / (2.0 * q);
            double cos = Math.cos(w);
            return new Biquad((1.0 - cos) / 2.0, 1.0 - cos, (1.0 - cos) / 2.0, 1.0 + alpha, -2.0 * cos, 1.0 - alpha);
        }

        static Biquad highPass(float frequency, float q) {
            double w = 2.0 * Math.PI * frequency / SAMPLE_RATE;
            double alpha = Math.sin(w) / (2.0 * q);
            double cos = Math.cos(w);
            return new Biquad((1.0 + cos) / 2.0, -(1.0 + cos), (1.0 + cos) / 2.0, 1.0 + alpha, -2.0 * cos, 1.0 - alpha);
        }

        static Biquad peak(float frequency, float q, float gainDb) {
            double w = 2.0 * Math.PI * frequency / SAMPLE_RATE;
            double alpha = Math.sin(w) / (2.0 * q);
            double cos = Math.cos(w);
            double a = Math.pow(10.0, gainDb / 40.0);
            return new Biquad(1.0 + alpha * a, -2.0 * cos, 1.0 - alpha * a, 1.0 + alpha / a, -2.0 * cos, 1.0 - alpha / a);
        }

        float process(float x) {
            float y = this.b0 * x + this.b1 * this.x1 + this.b2 * this.x2 - this.a1 * this.y1 - this.a2 * this.y2;
            this.x2 = this.x1;
            this.x1 = x;
            this.y2 = this.y1;
            this.y1 = y;
            return y;
        }

        void reset() {
            this.x1 = this.x2 = this.y1 = this.y2 = 0.0F;
        }
    }
}
