package dev.amble.client.hud;

import dev.amble.BrightestDay;
import dev.amble.client.screens.LanternWidgets;
import dev.amble.core.progression.Pilgrimage;
import dev.amble.core.ringpowers.LanternCorps;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

public final class PilgrimageHud {
    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 3;
    private static final int TOP = 4;

    public static void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, BrightestDay.id("pilgrimage"), PilgrimageHud::extract);
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isSpectator()) return;
        Pilgrimage.State state = player.getAttachedOrElse(Pilgrimage.STATE, Pilgrimage.State.NONE);
        if (!state.active() || state.total() <= 0) return;

        int color = ARGB.opaque(LanternCorps.BLUE.color());
        int center = graphics.guiWidth() / 2;
        Component title = Component.translatable("hud.brightestday.pilgrimage", state.next(), state.total());
        graphics.text(minecraft.font, title, center - minecraft.font.width(title) / 2, TOP, color, true);

        int x = center - BAR_WIDTH / 2;
        int y = TOP + 11;
        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, ARGB.color(0xA0, 0));
        graphics.fill(x, y, x + Math.round(BAR_WIDTH * Math.min(1.0F, state.next() / (float) state.total())), y + BAR_HEIGHT, color);

        state.target().ifPresent(target -> {
            int distance = (int) Math.sqrt(player.blockPosition().distSqr(new BlockPos(target.getX(), player.getBlockY(), target.getZ())));
            Component next = Component.translatable(state.complete() ? "hud.brightestday.pilgrimage.sanctuary" : "hud.brightestday.pilgrimage.next", distance);
            graphics.text(minecraft.font, next, center - minecraft.font.width(next) / 2, y + BAR_HEIGHT + 3, LanternWidgets.TEXT_DIM, true);
        });
    }

    private PilgrimageHud() {}
}
