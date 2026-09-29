package dev.amble.client.screens;

import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.networking.payloads.c2s.SetColorTweakC2SPayload;
import dev.amble.core.ringpowers.ColorTweak;
import dev.amble.core.ringpowers.CorpsColors;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import dev.amble.core.menus.LanternMenu;
import dev.amble.core.ringpowers.LanternCorps;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class LanternScreen extends AbstractContainerScreen<LanternMenu> {
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 186;
    private static final int SLIDER_X = 8;
    private static final int SLIDER_WIDTH = 160;
    private static final int SLIDER_HEIGHT = 16;
    private static final int BRIGHTNESS_Y = 50;
    private static final int SATURATION_Y = 70;
    private static final int INFO_X = 50;
    private static final int BAR_Y = 36;
    private static final int BAR_WIDTH = 118;
    private static final int BAR_HEIGHT = 5;

    private static final int PANEL = 0xFFC6C6C6;
    private static final int HIGHLIGHT = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF555555;
    private static final int OUTLINE = 0xFF000000;
    private static final int LABEL = 0xFF404040;

    private @Nullable ColorTweakSlider brightness;
    private @Nullable ColorTweakSlider saturation;

    public LanternScreen(LanternMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.inventoryLabelY = IMAGE_HEIGHT - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(new IconButton(
                this.leftPos - IconButton.SIZE - 2, this.topPos + 4,
                new ItemStack(Items.CRAFTING_TABLE),
                Component.translatable("gui.brightestday.inventory"),
                this::returnToInventory
        ));

        ColorTweak tweak = BrightestDayAttachments.getColorTweak(this.minecraft.player);
        this.brightness = this.addRenderableWidget(new ColorTweakSlider(
                this.leftPos + SLIDER_X, this.topPos + BRIGHTNESS_Y, SLIDER_WIDTH, SLIDER_HEIGHT,
                "gui.brightestday.brightness", tweak.brightness(), value -> this.updateTweak()));
        this.saturation = this.addRenderableWidget(new ColorTweakSlider(
                this.leftPos + SLIDER_X, this.topPos + SATURATION_Y, SLIDER_WIDTH, SLIDER_HEIGHT,
                "gui.brightestday.saturation", tweak.saturation(), value -> this.updateTweak()));
    }

    private void updateTweak() {
        if (this.brightness == null || this.saturation == null) return;

        ColorTweak tweak = new ColorTweak(this.brightness.tweak(), this.saturation.tweak());
        if (tweak.equals(BrightestDayAttachments.getColorTweak(this.minecraft.player))) return;

        BrightestDayAttachments.setColorTweak(this.minecraft.player, tweak);
        ClientPlayNetworking.send(new SetColorTweakC2SPayload(tweak));
    }

    private int tint(LanternCorps corps) {
        return ARGB.opaque(CorpsColors.apply(corps.color(), BrightestDayAttachments.getColorTweak(this.minecraft.player)));
    }

    private void returnToInventory() {
        this.minecraft.player.closeContainer();
        this.minecraft.gui.setScreen(new InventoryScreen(this.minecraft.player));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        drawPanel(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

        for (Slot slot : this.menu.slots) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, this.leftPos + slot.x - 1, this.topPos + slot.y - 1, 18, 18);
        }

        ItemStack ring = this.menu.getRing();
        Optional<LanternCorps> corps = PowerRingItem.getCorps(ring);
        int color = corps.map(this::tint).orElse(SHADOW);

        int slotX = this.leftPos + LanternMenu.RING_SLOT_X - 2;
        int slotY = this.topPos + LanternMenu.RING_SLOT_Y - 2;
        if (corps.isPresent()) graphics.outline(slotX, slotY, 20, 20, color);

        int barX = this.leftPos + INFO_X;
        int barY = this.topPos + BAR_Y;
        graphics.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, SHADOW);
        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xFF373737);
        if (corps.isPresent()) {
            int filled = Math.round(BAR_WIDTH * PowerRingItem.getChargeFraction(ring));
            graphics.fill(barX, barY, barX + filled, barY + BAR_HEIGHT, color);
            graphics.fill(barX, barY, barX + filled, barY + 1, ARGB.color(96, 255, 255, 255));
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);

        ItemStack ring = this.menu.getRing();
        Optional<LanternCorps> corps = PowerRingItem.getCorps(ring);
        if (corps.isEmpty()) {
            graphics.text(this.font, Component.translatable("gui.brightestday.no_ring"), INFO_X, BAR_Y - 12, LABEL, false);
            return;
        }

        graphics.text(this.font, Component.translatable(corps.get().getTranslationKey()), INFO_X, BAR_Y - 12, this.tint(corps.get()), false);

        int percent = Math.round(PowerRingItem.getChargeFraction(ring) * 100);
        String charge = percent + "%";
        graphics.text(this.font, charge, INFO_X + BAR_WIDTH - this.font.width(charge), BAR_Y - 12, LABEL, false);
    }

    private static void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        int x1 = x + width;
        int y1 = y + height;
        graphics.fill(x + 1, y + 1, x1 - 1, y1 - 1, PANEL);
        graphics.fill(x + 1, y, x1 - 1, y + 1, OUTLINE);
        graphics.fill(x + 1, y1 - 1, x1 - 1, y1, OUTLINE);
        graphics.fill(x, y + 1, x + 1, y1 - 1, OUTLINE);
        graphics.fill(x1 - 1, y + 1, x1, y1 - 1, OUTLINE);
        graphics.fill(x + 1, y + 1, x1 - 3, y + 3, HIGHLIGHT);
        graphics.fill(x + 1, y + 1, x + 3, y1 - 3, HIGHLIGHT);
        graphics.fill(x + 3, y1 - 3, x1 - 1, y1 - 1, SHADOW);
        graphics.fill(x1 - 3, y + 3, x1 - 1, y1 - 1, SHADOW);
    }
}
