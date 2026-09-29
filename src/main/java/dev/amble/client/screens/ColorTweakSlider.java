package dev.amble.client.screens;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;

public class ColorTweakSlider extends AbstractSliderButton {
    private final String translationKey;
    private final DoubleConsumer onChange;

    public ColorTweakSlider(int x, int y, int width, int height, String translationKey, float tweak, DoubleConsumer onChange) {
        super(x, y, width, height, Component.empty(), (tweak + 1.0) / 2.0);
        this.translationKey = translationKey;
        this.onChange = onChange;
        this.updateMessage();
    }

    public float tweak() {
        return (float) (this.value * 2.0 - 1.0);
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
