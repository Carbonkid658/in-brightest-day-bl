package dev.amble.client.screens;

import dev.amble.BrightestDay;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.resources.Identifier;

public final class LanternWidgets {
    public static final Identifier BACKGROUND = sprite("background");
    public static final Identifier SLOT = sprite("slot");
    public static final Identifier RING_SLOT = sprite("ring_slot");
    public static final WidgetSprites BUTTON = new WidgetSprites(sprite("button"), sprite("button_disabled"), sprite("button_highlighted"));
    public static final Identifier SLIDER = sprite("slider");
    public static final Identifier SLIDER_HIGHLIGHTED = sprite("slider_highlighted");
    public static final Identifier SLIDER_HANDLE = sprite("slider_handle");
    public static final Identifier SLIDER_HANDLE_HIGHLIGHTED = sprite("slider_handle_highlighted");
    public static final Identifier TOGGLE = sprite("toggle");
    public static final Identifier TOGGLE_HIGHLIGHTED = sprite("toggle_highlighted");
    public static final Identifier TOGGLE_SELECTED = sprite("toggle_selected");
    public static final Identifier TOGGLE_SELECTED_HIGHLIGHTED = sprite("toggle_selected_highlighted");

    public static final int TEXT = 0xFFF2DD96;
    public static final int TEXT_DIM = 0xFFB8A66A;
    public static final int BAR_FRAME = 0xFF8C5A1E;
    public static final int BAR_EMPTY = 0xFF061109;

    private static Identifier sprite(String name) {
        return BrightestDay.id("lantern/" + name);
    }

    private LanternWidgets() {}
}
