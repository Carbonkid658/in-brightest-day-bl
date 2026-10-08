package dev.amble.client.hud;

import dev.amble.BrightestDay;
import dev.amble.client.wheel.PowerWheel;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.sync.RingSync;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public final class SyncHud {
    private static final int GAP = 3;
    private static final int BAR_WIDTH = 40;
    private static final int BAR_HEIGHT = 3;
    private static final int DEGRADED_COLOR = 0xFFFFB347;
    private static final int UNSTABLE_COLOR = 0xFFFF7A5A;
    private static final int LOCKED_COLOR = 0xFFFF4040;

    public static void init() {
        HudElementRegistry.attachElementAfter(BrightestDay.id("power_indicator"), BrightestDay.id("ring_sync"), SyncHud::extract);
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;
        float sync = RingSync.sync(player);
        if (sync >= RingSync.DEGRADED) return;

        Font font = client.font;
        boolean orange = PowerRingItem.getWornCorps(player).orElse(null) == LanternCorps.ORANGE;
        Component home = Component.translatable(orange ? "message.brightestday.sync.home.lantern" : "message.brightestday.sync.home.battery");
        String key = sync <= 0.0F ? "hud.brightestday.sync.locked" : "hud.brightestday.sync.low";
        Component text = Component.translatable(key, RingSync.percent(sync), home);
        int color = sync <= 0.0F ? LOCKED_COLOR : sync < RingSync.UNSTABLE ? UNSTABLE_COLOR : DEGRADED_COLOR;

        float time = player.tickCount + deltaTracker.getGameTimeDeltaPartialTick(false);
        float flicker = sync < RingSync.UNSTABLE ? 0.65F + 0.35F * Mth.abs(Mth.sin(time * 0.35F)) : 1.0F;
        int anchor = PowerWheel.indicatorTop();
        int bottom = (anchor >= 0 ? anchor : graphics.guiHeight() - PowerWheel.HUD_MARGIN) - GAP;
        int left = PowerWheel.HUD_MARGIN;
        int textY = bottom - BAR_HEIGHT - 2 - font.lineHeight;

        graphics.text(font, text, left, textY, ARGB.multiplyAlpha(color, flicker), true);
        graphics.fill(left, bottom - BAR_HEIGHT, left + BAR_WIDTH, bottom, 0xA0000000);
        graphics.fill(left, bottom - BAR_HEIGHT, left + Math.round(BAR_WIDTH * sync), bottom, ARGB.multiplyAlpha(color, flicker));
    }

    private SyncHud() {}
}
