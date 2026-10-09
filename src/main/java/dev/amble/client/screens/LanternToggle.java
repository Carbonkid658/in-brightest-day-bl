package dev.amble.client.screens;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class LanternToggle extends AbstractButton {
    public static final int BOX_SIZE = 17;
    private static final int LABEL_GAP = 4;

    private final Font font;
    private final Consumer<Boolean> onChange;
    private boolean selected;

    public LanternToggle(int x, int y, Component message, Font font, boolean selected, Consumer<Boolean> onChange) {
        super(x, y, width(font, message), BOX_SIZE, message);
        this.font = font;
        this.selected = selected;
        this.onChange = onChange;
    }

    public static int width(Font font, Component message) {
        return BOX_SIZE + LABEL_GAP + font.width(message);
    }

    public boolean selected() {
        return this.selected;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.selected = !this.selected;
        this.onChange.accept(this.selected);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        boolean highlighted = this.isHoveredOrFocused();
        LanternWidgets.toggle(graphics, this.getX(), this.getY(), BOX_SIZE, LanternWidgets.accent(), this.selected, highlighted, this.alpha);
        graphics.text(this.font, this.getMessage(), this.getX() + BOX_SIZE + LABEL_GAP, this.getY() + (BOX_SIZE - this.font.lineHeight) / 2 + 1,
                highlighted ? LanternWidgets.TEXT : LanternWidgets.TEXT_DIM, true);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.createNarrationMessage());
        output.add(NarratedElementType.USAGE, Component.translatable(this.selected ? "narration.checkbox.usage.focused.uncheck" : "narration.checkbox.usage.focused.check"));
    }
}
