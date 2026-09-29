package dev.amble.client.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class IconButton extends AbstractButton {
    public static final int SIZE = 20;

    private final ItemStack icon;
    private final Runnable onPress;

    public IconButton(int x, int y, ItemStack icon, Component message, Runnable onPress) {
        super(x, y, SIZE, SIZE, message);
        this.icon = icon;
        this.onPress = onPress;
        this.setTooltip(Tooltip.create(message));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.onPress.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractDefaultSprite(graphics);
        graphics.item(this.icon, this.getX() + 2, this.getY() + 2);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
