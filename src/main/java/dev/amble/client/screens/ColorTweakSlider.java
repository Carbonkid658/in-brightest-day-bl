package dev.amble.client.screens;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.DoubleConsumer;

public class ColorTweakSlider extends AbstractSliderButton {
    private final String translationKey;
    private final float min;
    private final float max;
    private final DoubleConsumer onChange;

    public ColorTweakSlider(int x, int y, int width, int height, String translationKey, float tweak, float min, float max, DoubleConsumer onChange) {
        super(x, y, width, height, Component.empty(), Mth.clamp((tweak - min) / (max - min), 0.0, 1.0));
        this.translationKey = translationKey;
        this.min = min;
        this.max = max;
        this.onChange = onChange;
        this.updateMessage();
    }

    public float tweak() {
        return (float) Mth.lerp(this.value, this.min, this.max);
    }

    @Override
    protected void updateMessage() {
        int percent = Math.round(this.tweak() * 100.0F);
        this.setMessage(Component.translatable(this.translationKey, (percent > 0 ? "+" : "") + percent + "%"));
    }

    @Override
    protected void applyValue() {
        this.onChange.accept(this.tweak());
    }
}
