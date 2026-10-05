package dev.amble.client.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public abstract class LanternSlider extends AbstractSliderButton {
    protected LanternSlider(int x, int y, int width, int height, double value) {
        super(x, y, width, height, Component.empty(), value);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        boolean highlighted = this.isActive() && this.isHoveredOrFocused();
        int accent = LanternWidgets.accent();
        LanternWidgets.track(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), accent, highlighted, this.alpha);
        LanternWidgets.handle(graphics, this.getX() + (int) (this.value * (this.width - HANDLE_WIDTH)), this.getY(), HANDLE_WIDTH, this.getHeight(),
                accent, highlighted, this.alpha);
        this.extractScrollingStringOverContents(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE),
                this.getMessage().copy().withColor(LanternWidgets.TEXT), TEXT_MARGIN);
        this.handleCursor(graphics);
    }
}
