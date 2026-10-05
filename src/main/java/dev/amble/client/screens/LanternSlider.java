package dev.amble.client.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

public abstract class LanternSlider extends AbstractSliderButton {
    protected LanternSlider(int x, int y, int width, int height, double value) {
        super(x, y, width, height, Component.empty(), value);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        boolean highlighted = this.isActive() && this.isHoveredOrFocused();
        int tint = ARGB.white(this.alpha);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, highlighted ? LanternWidgets.SLIDER_HIGHLIGHTED : LanternWidgets.SLIDER,
                this.getX(), this.getY(), this.getWidth(), this.getHeight(), tint);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, highlighted ? LanternWidgets.SLIDER_HANDLE_HIGHLIGHTED : LanternWidgets.SLIDER_HANDLE,
                this.getX() + (int) (this.value * (this.width - HANDLE_WIDTH)), this.getY(), HANDLE_WIDTH, this.getHeight(), tint);
        this.extractScrollingStringOverContents(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE),
                this.getMessage().copy().withColor(LanternWidgets.TEXT), TEXT_MARGIN);
        this.handleCursor(graphics);
    }
}
