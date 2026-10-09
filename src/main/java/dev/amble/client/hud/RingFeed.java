package dev.amble.client.hud;

import dev.amble.BrightestDay;
import dev.amble.client.effects.CommsClient;
import dev.amble.client.wheel.PowerWheel;
import dev.amble.core.ringpowers.CorpsColors;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class RingFeed {
    private static final String[] PREFIXES = {"message.brightestday.", "hud.brightestday."};
    private static final int MAX_ENTRIES = 4;
    private static final int LIFETIME_TICKS = 70;
    private static final int FADE_TICKS = 12;
    private static final int PADDING = 3;
    private static final int GAP = 2;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int BACKGROUND_ALPHA = 0x90;

    public record Line(Component text, int color) {}

    private static final class Entry {
        final String key;
        Component text;
        int age;

        Entry(String key, Component text) {
            this.key = key;
            this.text = text;
        }
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(RingFeed::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ENTRIES.clear());
        HudElementRegistry.attachElementAfter(BrightestDay.id("power_indicator"), BrightestDay.id("ring_feed"), RingFeed::extract);
    }

    public static boolean accept(Component message) {
        String key = key(message);
        if (key == null) return false;
        for (Entry entry : ENTRIES) {
            if (!entry.key.equals(key)) continue;
            entry.text = message;
            entry.age = 0;
            ENTRIES.remove(entry);
            ENTRIES.add(entry);
            return true;
        }
        ENTRIES.add(new Entry(key, message));
        while (ENTRIES.size() > MAX_ENTRIES) ENTRIES.removeFirst();
        return true;
    }

    private static @Nullable String key(Component message) {
        Component source = message.getContents() instanceof TranslatableContents ? message
                : message.getSiblings().isEmpty() ? message : message.getSiblings().getFirst();
        if (!(source.getContents() instanceof TranslatableContents translatable)) return null;
        for (String prefix : PREFIXES) {
            if (translatable.getKey().startsWith(prefix)) return translatable.getKey();
        }
        return null;
    }

    private static void tick(Minecraft client) {
        if (client.isPaused()) return;
        ENTRIES.removeIf(entry -> ++entry.age > LIFETIME_TICKS);
    }

    private record Row(Component text, int color, float alpha) {}

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        Font font = client.font;
        int accent = ARGB.opaque(CorpsColors.ui(player));
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);

        List<Row> rows = new ArrayList<>();
        for (Line line : CommsClient.pinned(player)) rows.add(new Row(line.text(), line.color(), 1.0F));
        for (Entry entry : ENTRIES) {
            float remaining = LIFETIME_TICKS - entry.age - partialTicks;
            float alpha = Mth.clamp(remaining / FADE_TICKS, 0.0F, 1.0F);
            if (alpha <= 0.02F) continue;
            TextColor style = entry.text.getStyle().getColor();
            rows.add(new Row(entry.text, style != null ? ARGB.opaque(style.getValue()) : TEXT_COLOR, alpha));
        }
        if (rows.isEmpty()) return;

        int height = font.lineHeight + PADDING * 2;
        int top = graphics.guiHeight() / 2 - (rows.size() * (height + GAP) - GAP) / 2;
        for (Row row : rows) {
            draw(graphics, font, row.text(), row.color(), accent, row.alpha(), top, height);
            top += height + GAP;
        }
    }

    private static void draw(GuiGraphicsExtractor graphics, Font font, Component text, int color, int accent, float alpha, int top, int height) {
        int right = graphics.guiWidth() - PowerWheel.HUD_MARGIN;
        int width = font.width(text) + PADDING * 2 + 1;
        int left = right - width;
        int bottom = top + height;
        graphics.fill(left, top, right, bottom, ARGB.color(Math.round(BACKGROUND_ALPHA * alpha), ARGB.scaleRGB(accent, 0.25F)));
        graphics.fill(right - 1, top, right, bottom, ARGB.multiplyAlpha(accent, alpha));
        graphics.text(font, text, left + PADDING, top + PADDING + 1, ARGB.multiplyAlpha(color, alpha), true);
    }

    private RingFeed() {}
}
