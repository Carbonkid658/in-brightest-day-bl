package dev.amble.client.screens;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;

public class MaskHeightSlider extends LanternSlider {
    private final int range;
    private final IntConsumer onChange;

    public MaskHeightSlider(int x, int y, int width, int height, int offset, int range, IntConsumer onChange) {
        super(x, y, width, height, (offset + range) / (2.0 * range));
        this.range = range;
        this.onChange = onChange;
        this.updateMessage();
    }

    public int offset() {
        return Mth.clamp((int) Math.round(this.value * 2 * this.range) - this.range, -this.range, this.range);
    }

    @Override
    protected void updateMessage() {
        int offset = this.offset();
        this.setMessage(Component.translatable("gui.brightestday.mask_height", (offset > 0 ? "+" : "") + offset));
    }

    @Override
    protected void applyValue() {
        this.value = (this.offset() + this.range) / (2.0 * this.range);
        this.onChange.accept(this.offset());
    }
}
