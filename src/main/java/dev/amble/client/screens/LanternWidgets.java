package dev.amble.client.screens;

import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.CorpsColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.ARGB;

public final class LanternWidgets {
    public static final int TEXT = 0xFFF2DD96;
    public static final int TEXT_DIM = 0xFFB8A66A;
    public static final int BAR_FRAME = 0xFF8C5A1E;
    public static final int BAR_EMPTY = 0xFF061109;

    private static final int NEUTRAL = 0xFF8A8A92;
    private static final int SHADOW = 0xFF050507;
    private static final int DEEP = 0xFF0A0A0D;

    public static int accent() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return NEUTRAL;
        return PowerRingItem.getCorps(BrightestDayAttachments.getRing(player))
                .map(corps -> ARGB.opaque(CorpsColors.apply(corps.color(), BrightestDayAttachments.getColorTweak(player))))
                .orElse(NEUTRAL);
    }

    public static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int accent) {
        graphics.fill(x, y, x + width, y + height, SHADOW);
        graphics.fillGradient(x + 1, y + 1, x + width - 1, y + height - 1, shade(accent, 0.3F), DEEP);
        graphics.fillGradient(x + 1, y + 1, x + width / 2, y + height / 3, ARGB.color(0x30, accent), ARGB.transparent(accent));
        graphics.outline(x + 2, y + 2, width - 4, height - 4, ARGB.color(0xB0, accent));
        graphics.fill(x + 3, y + 3, x + width - 3, y + 4, ARGB.color(0x50, ARGB.srgbLerp(0.5F, accent, 0xFFFFFFFF)));
    }

    public static void divider(GuiGraphicsExtractor graphics, int x0, int x1, int y, int accent) {
        if (x1 <= x0) return;
        int mid = (x0 + x1) / 2;
        graphics.fill(x0, y, x1, y + 1, ARGB.color(0x90, accent));
        graphics.fill(mid - 1, y - 1, mid + 2, y + 2, ARGB.srgbLerp(0.6F, accent, 0xFFFFFFFF));
    }

    public static void slot(GuiGraphicsExtractor graphics, int x, int y, int size, int accent) {
        graphics.fill(x, y, x + size, y + size, ARGB.color(0x60, accent));
        graphics.fillGradient(x + 1, y + 1, x + size - 1, y + size - 1, SHADOW, shade(accent, 0.12F));
    }

    public static void ringSlot(GuiGraphicsExtractor graphics, int x, int y, int size, int accent) {
        graphics.fill(x, y, x + size, y + size, SHADOW);
        graphics.outline(x + 1, y + 1, size - 2, size - 2, accent);
        graphics.fillGradient(x + 2, y + 2, x + size - 2, y + size - 2, shade(accent, 0.35F), SHADOW);
    }

    public static void button(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int accent, boolean active, boolean hovered, float alpha) {
        int edge = !active ? NEUTRAL : hovered ? ARGB.srgbLerp(0.45F, accent, 0xFFFFFFFF) : accent;
        int top = !active ? 0xFF2A2A2E : shade(accent, hovered ? 0.55F : 0.35F);
        graphics.fill(x, y, x + width, y + height, ARGB.multiplyAlpha(SHADOW, alpha));
        graphics.fillGradient(x + 1, y + 1, x + width - 1, y + height - 1, ARGB.multiplyAlpha(top, alpha), ARGB.multiplyAlpha(DEEP, alpha));
        graphics.outline(x, y, width, height, ARGB.multiplyAlpha(edge, alpha));
    }

    public static void track(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int accent, boolean hovered, float alpha) {
        graphics.fill(x, y, x + width, y + height, ARGB.multiplyAlpha(SHADOW, alpha));
        graphics.fillGradient(x + 1, y + 1, x + width - 1, y + height - 1,
                ARGB.multiplyAlpha(DEEP, alpha), ARGB.multiplyAlpha(shade(accent, 0.2F), alpha));
        graphics.outline(x, y, width, height, ARGB.multiplyAlpha(ARGB.color(hovered ? 0xFF : 0x90, accent), alpha));
    }

    public static void handle(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int accent, boolean hovered, float alpha) {
        int face = hovered ? ARGB.srgbLerp(0.4F, accent, 0xFFFFFFFF) : accent;
        graphics.fill(x, y, x + width, y + height, ARGB.multiplyAlpha(SHADOW, alpha));
        graphics.fillGradient(x + 1, y + 1, x + width - 1, y + height - 1, ARGB.multiplyAlpha(face, alpha), ARGB.multiplyAlpha(shade(accent, 0.5F), alpha));
    }

    public static void toggle(GuiGraphicsExtractor graphics, int x, int y, int size, int accent, boolean selected, boolean hovered, float alpha) {
        int edge = hovered ? ARGB.srgbLerp(0.45F, accent, 0xFFFFFFFF) : accent;
        graphics.fill(x, y, x + size, y + size, ARGB.multiplyAlpha(SHADOW, alpha));
        graphics.fillGradient(x + 1, y + 1, x + size - 1, y + size - 1, ARGB.multiplyAlpha(DEEP, alpha), ARGB.multiplyAlpha(shade(accent, 0.2F), alpha));
        graphics.outline(x, y, size, size, ARGB.multiplyAlpha(edge, alpha));
        if (selected) {
            graphics.fillGradient(x + 4, y + 4, x + size - 4, y + size - 4,
                    ARGB.multiplyAlpha(ARGB.srgbLerp(0.3F, accent, 0xFFFFFFFF), alpha), ARGB.multiplyAlpha(accent, alpha));
        }
    }

    private static int shade(int color, float amount) {
        return ARGB.srgbLerp(amount, 0xFF000000, ARGB.opaque(color));
    }

    private LanternWidgets() {}
}
