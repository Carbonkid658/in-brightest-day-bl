package dev.amble.client.screens;

import dev.amble.BrightestDay;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;

public final class OfficialServerButton {
    private static final String MULTIPLAYER = "menu.multiplayer";
    private static final int GAP = 4;
    private static final Identifier ICON = BrightestDay.id("textures/item/green_power_ring.png");

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof TitleScreen)) return;
            AbstractWidget multiplayer = null;
            for (AbstractWidget widget : Screens.getWidgets(screen)) {
                if (widget.getMessage().getContents() instanceof TranslatableContents contents && contents.getKey().equals(MULTIPLAYER)) {
                    multiplayer = widget;
                    break;
                }
            }
            int x = multiplayer == null ? scaledWidth / 2 + 104 : multiplayer.getX() + multiplayer.getWidth() + GAP;
            int y = multiplayer == null ? scaledHeight / 4 + 72 : multiplayer.getY();
            Screens.getWidgets(screen).add(new IconButton(x, y, ICON,
                    Component.translatable("gui.brightestday.official.button"), () -> client.gui.setScreen(new OfficialServerScreen(screen))));
        });
    }

    private OfficialServerButton() {}
}
