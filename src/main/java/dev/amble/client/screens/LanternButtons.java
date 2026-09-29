package dev.amble.client.screens;

import dev.amble.core.BrightestDayItems;
import dev.amble.core.networking.payloads.c2s.OpenLanternC2SPayload;
import dev.amble.mixin.client.AbstractContainerScreenAccessor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class LanternButtons {
    private static final int RECIPE_BOOK_WIDTH = 147;
    private static final int GAP = 2;

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen)) return;

            IconButton button = new IconButton(0, 0,
                    new ItemStack(BrightestDayItems.GREEN_POWER_RING),
                    Component.translatable("gui.brightestday.lantern"),
                    () -> ClientPlayNetworking.send(OpenLanternC2SPayload.INSTANCE));

            position(screen, button);
            Screens.getWidgets(screen).add(button);
            ScreenEvents.beforeExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> position(s, button));
        });
    }

    private static void position(Screen screen, IconButton button) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) (AbstractContainerScreen<?>) screen;
        int leftPos = accessor.brightestday$getLeftPos();
        boolean recipeBookOpen = leftPos > (screen.width - accessor.brightestday$getImageWidth()) / 2;
        int edge = recipeBookOpen ? leftPos - RECIPE_BOOK_WIDTH - GAP : leftPos;

        button.setX(edge - IconButton.SIZE - GAP);
        button.setY(accessor.brightestday$getTopPos() + 4);
    }

    private LanternButtons() {}
}
