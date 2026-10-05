package dev.amble.client.screens;

import dev.amble.core.BrightestDayItems;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.CorpsColors;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.networking.payloads.c2s.OpenLanternC2SPayload;
import dev.amble.mixin.client.AbstractContainerScreenAccessor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class LanternButtons {
    private static final int RECIPE_BOOK_WIDTH = 147;
    private static final int GAP = 2;

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen)) return;

            IconButton button = new IconButton(0, 0,
                    new ItemStack(BrightestDayItems.GREEN_POWER_RING),
                    Component.translatable("gui.brightestday.lantern"),
                    () -> openLantern(screen));

            position(screen, button);
            Screens.getWidgets(screen).add(button);
            int[] shownCharge = {Integer.MIN_VALUE};
            ScreenEvents.beforeExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
                position(s, button);
                updateTooltip(client, button, shownCharge);
            });
        });
    }

    private static void openLantern(Screen screen) {
        if (!(screen instanceof CreativeModeInventoryScreen creative)) {
            ClientPlayNetworking.send(OpenLanternC2SPayload.INSTANCE);
            return;
        }

        ItemStack carried = creative.getMenu().getCarried();
        creative.getMenu().setCarried(ItemStack.EMPTY);
        ClientPlayNetworking.send(new OpenLanternC2SPayload(carried.copy()));
    }

    private static void updateTooltip(Minecraft client, IconButton button, int[] shownCharge) {
        if (client.player == null) return;
        ItemStack ring = PowerRingItem.getWornRing(client.player);
        Optional<LanternCorps> corps = PowerRingItem.getCorps(ring);
        int charge = corps.isEmpty() ? -1 : !PowerRingItem.usesPower(ring) ? -2 : Math.round(PowerRingItem.getChargeFraction(ring) * 100);
        if (charge == shownCharge[0]) return;
        shownCharge[0] = charge;

        Component detail = charge == -2
                ? corps.get().displayName().copy().withColor(CorpsColors.of(client.player))
                : corps.isPresent()
                ? Component.translatable("gui.brightestday.ring_charge", charge).withColor(CorpsColors.of(client.player))
                : Component.translatable("gui.brightestday.no_ring").withStyle(ChatFormatting.GRAY);
        button.setTooltip(Tooltip.create(Component.translatable("gui.brightestday.lantern").append("\n").append(detail)));
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
