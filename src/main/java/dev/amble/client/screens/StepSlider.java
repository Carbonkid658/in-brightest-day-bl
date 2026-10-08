package dev.amble.client.screens;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;

public class StepSlider extends LanternSlider {
    private final int steps;
    private final IntFunction<Component> label;
    private final IntConsumer onChange;

    public StepSlider(int x, int y, int width, int height, int step, int steps, IntFunction<Component> label, IntConsumer onChange) {
        super(x, y, width, height, steps <= 0 ? 0.0 : step / (double) steps);
        this.steps = steps;
        this.label = label;
        this.onChange = onChange;
        this.updateMessage();
    }

    public int step() {
        return Mth.clamp((int) Math.round(this.value * this.steps), 0, this.steps);
    }

    @Override
    protected void updateMessage() {
        this.setMessage(this.label.apply(this.step()));
    }

    @Override
    protected void applyValue() {
        this.value = this.steps <= 0 ? 0.0 : this.step() / (double) this.steps;
        this.onChange.accept(this.step());
    }
}
