package dev.amble.client.screens;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class AuraToggle extends AbstractButton {
    private static final int LABEL_GAP = 4;

    public enum Mode {
        OFF("gui.brightestday.aura"),
        FLYING("gui.brightestday.aura.flying"),
        ALWAYS("gui.brightestday.aura.always");

        private final String key;

        Mode(String key) {
            this.key = key;
        }

        public Component label() {
            return Component.translatable(this.key);
        }

        public Mode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }

        public static Mode of(boolean aura, boolean flightOnly) {
            return !aura ? OFF : flightOnly ? FLYING : ALWAYS;
        }
    }

    private final Font font;
    private final Consumer<Mode> onChange;
    private Mode mode;

    public AuraToggle(int x, int y, Font font, Mode mode, Consumer<Mode> onChange) {
        super(x, y, width(font), LanternToggle.BOX_SIZE, mode.label());
        this.font = font;
        this.mode = mode;
        this.onChange = onChange;
    }

    public static int width(Font font) {
        int widest = 0;
        for (Mode mode : Mode.values()) widest = Math.max(widest, font.width(mode.label()));
        return LanternToggle.BOX_SIZE + LABEL_GAP + widest;
    }

    public Mode mode() {
        return this.mode;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.mode = this.mode.next();
        this.setMessage(this.mode.label());
        this.onChange.accept(this.mode);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        boolean highlighted = this.isHoveredOrFocused();
        LanternWidgets.toggle(graphics, this.getX(), this.getY(), LanternToggle.BOX_SIZE, LanternWidgets.accent(), this.mode != Mode.OFF, highlighted, this.alpha);
        graphics.text(this.font, this.getMessage(), this.getX() + LanternToggle.BOX_SIZE + LABEL_GAP, this.getY() + (LanternToggle.BOX_SIZE - this.font.lineHeight) / 2 + 1,
                highlighted ? LanternWidgets.TEXT : LanternWidgets.TEXT_DIM, true);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.createNarrationMessage());
    }
}
