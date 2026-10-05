package dev.amble.client.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class IconButton extends AbstractButton {
    public static final int SIZE = 20;

    private final ItemStack icon;
    private final Runnable onPress;
    private final @Nullable WidgetSprites sprites;

    public IconButton(int x, int y, ItemStack icon, Component message, Runnable onPress) {
        this(x, y, icon, message, onPress, null);
    }

    public IconButton(int x, int y, ItemStack icon, Component message, Runnable onPress, @Nullable WidgetSprites sprites) {
        super(x, y, SIZE, SIZE, message);
        this.icon = icon;
        this.onPress = onPress;
        this.sprites = sprites;
        this.setTooltip(Tooltip.create(message));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.onPress.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (this.sprites == null) {
            this.extractDefaultSprite(graphics);
        } else {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprites.get(this.active, this.isHoveredOrFocused()),
                    this.getX(), this.getY(), this.getWidth(), this.getHeight(), ARGB.white(this.alpha));
        }
        graphics.item(this.icon, this.getX() + 2, this.getY() + 2);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
