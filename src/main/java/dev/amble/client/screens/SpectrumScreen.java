package dev.amble.client.screens;

import com.mojang.blaze3d.platform.InputConstants;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.progression.Emotion;
import dev.amble.core.progression.Milestone;
import dev.amble.core.progression.Milestones;
import dev.amble.core.progression.RingRanks;
import dev.amble.core.progression.SpectrumMeters;
import dev.amble.core.ringpowers.LanternCorps;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;


public class SpectrumScreen extends Screen {
    private static final int WIDTH = 304;
    private static final int HEIGHT = 214;
    private static final int MARGIN = 10;
    private static final int LIST_WIDTH = 92;
    private static final int ROW_HEIGHT = 24;
    private static final int LIST_Y = 28;
    private static final int DETAIL_X = MARGIN + LIST_WIDTH + 10;
    private static final int DETAIL_WIDTH = WIDTH - DETAIL_X - MARGIN;
    private static final int METER_HEIGHT = 6;
    private static final int TASK_BAR_HEIGHT = 3;
    private static final int DONE = 0xFF7DFF9A;
    private static final int LOCKED = 0xFF5E5A50;

    private final @Nullable Screen parent;
    private Emotion selected;
    private boolean chosen;
    private int left;
    private int top;

    public SpectrumScreen(@Nullable Screen parent) {
        super(Component.translatable("gui.brightestday.spectrum.title"));
        this.parent = parent;
        this.selected = Emotion.WILL;
    }

    @Override
    protected void init() {
        this.left = (this.width - WIDTH) / 2;
        this.top = (this.height - HEIGHT) / 2;
        if (!this.chosen && this.minecraft.player != null) {
            PowerRingItem.getWornCorps(this.minecraft.player).flatMap(Emotion::of).ifPresent(emotion -> this.selected = emotion);
        }
        this.chosen = true;
    }

    private int rowY(int index) {
        return this.top + LIST_Y + index * ROW_HEIGHT;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        Player player = this.minecraft.player;
        if (player == null) return;

        LanternCorps corps = this.selected.corps();
        int accent = ARGB.opaque(corps.color());
        LanternWidgets.panel(graphics, this.left, this.top, WIDTH, HEIGHT, accent);
        graphics.text(this.font, this.title, this.left + MARGIN, this.top + 11, LanternWidgets.TEXT, true);
        LanternWidgets.divider(graphics, this.left + MARGIN, this.left + WIDTH - MARGIN, this.top + 23, accent);

        this.list(graphics, player, mouseX, mouseY);
        this.detail(graphics, player, corps, accent);
    }

    private void list(GuiGraphicsExtractor graphics, Player player, int mouseX, int mouseY) {
        LanternCorps worn = PowerRingItem.getWornCorps(player).orElse(null);
        Emotion[] emotions = Emotion.values();
        for (int i = 0; i < emotions.length; i++) {
            Emotion emotion = emotions[i];
            int color = ARGB.opaque(emotion.corps().color());
            int x = this.left + MARGIN;
            int y = this.rowY(i);
            boolean hovered = mouseX >= x && mouseX < x + LIST_WIDTH && mouseY >= y && mouseY < y + ROW_HEIGHT - 2;
            boolean chosen = emotion == this.selected;

            if (chosen || hovered) graphics.fill(x, y, x + LIST_WIDTH, y + ROW_HEIGHT - 2, ARGB.color(chosen ? 0x50 : 0x28, color));
            graphics.fill(x, y, x + 3, y + ROW_HEIGHT - 2, color);

            Component name = emotion.corps().displayName();
            String label = this.font.plainSubstrByWidth(name.getString(), LIST_WIDTH - 22);
            graphics.text(this.font, label, x + 6, y + 3, chosen ? 0xFFFFFFFF : LanternWidgets.TEXT, true);
            graphics.text(this.font, String.valueOf(RingRanks.rank(player, emotion.corps())), x + LIST_WIDTH - 9, y + 3,
                    emotion.corps() == worn ? DONE : LanternWidgets.TEXT_DIM, true);

            int barWidth = LIST_WIDTH - 10;
            int filled = Math.round(barWidth * SpectrumMeters.get(player, emotion) / (float) Emotion.MAX);
            graphics.fill(x + 6, y + 14, x + 6 + barWidth, y + 16, LanternWidgets.BAR_EMPTY);
            graphics.fill(x + 6, y + 14, x + 6 + filled, y + 16, color);
        }
    }

    private void detail(GuiGraphicsExtractor graphics, Player player, LanternCorps corps, int accent) {
        int x = this.left + DETAIL_X;
        int y = this.top + LIST_Y;
        int rank = RingRanks.rank(player, corps);

        graphics.text(this.font, corps.displayName(), x, y, accent, true);
        Component rankText = Component.translatable("gui.brightestday.spectrum.rank", rank, RingRanks.MAX_RANK);
        graphics.text(this.font, rankText, x + DETAIL_WIDTH - this.font.width(rankText), y, LanternWidgets.TEXT, true);
        y += 12;

        for (FormattedCharSequence line : this.font.split(Component.translatable("gui.brightestday.spectrum.path." + corps.getSerializedName()), DETAIL_WIDTH)) {
            graphics.text(this.font, line, x, y, LanternWidgets.TEXT_DIM, false);
            y += 9;
        }
        y += 4;

        int meter = SpectrumMeters.get(player, this.selected);
        graphics.text(this.font, Component.translatable("gui.brightestday.spectrum.meter",
                Component.translatable("emotion.brightestday." + this.selected.getSerializedName()), meter, Emotion.MAX), x, y, LanternWidgets.TEXT, false);
        y += 10;
        graphics.fill(x - 1, y - 1, x + DETAIL_WIDTH + 1, y + METER_HEIGHT + 1, LanternWidgets.BAR_FRAME);
        graphics.fill(x, y, x + DETAIL_WIDTH, y + METER_HEIGHT, LanternWidgets.BAR_EMPTY);
        graphics.fillGradient(x, y, x + Math.round(DETAIL_WIDTH * meter / (float) Emotion.MAX), y + METER_HEIGHT,
                ARGB.srgbLerp(0.4F, accent, 0xFFFFFFFF), accent);
        int gate = x + DETAIL_WIDTH * Emotion.GATE / Emotion.MAX;
        graphics.fill(gate, y - 2, gate + 1, y + METER_HEIGHT + 2, meter >= Emotion.GATE ? DONE : 0xFFFFFFFF);
        y += METER_HEIGHT + 4;

        int capacity = Math.round(RingRanks.capacityFraction(rank) * 100);
        int arsenal = Math.round(RingRanks.arsenalFraction(rank) * 100);
        graphics.text(this.font, Component.translatable("gui.brightestday.spectrum.capacity", capacity, arsenal), x, y, LanternWidgets.TEXT_DIM, false);
        y += 14;

        graphics.text(this.font, Component.translatable("gui.brightestday.spectrum.milestones"), x, y, LanternWidgets.TEXT, true);
        y += 11;
        RingRanks.Ranks ranks = RingRanks.get(player);
        for (int tier = Milestones.FIRST_TIER; tier <= Milestones.LAST_TIER; tier++) {
            Milestone milestone = ranks.milestone(corps, tier).orElse(null);
            if (milestone == null) {
                graphics.text(this.font, Component.translatable("gui.brightestday.spectrum.unrevealed", tier), x + 9, y, LOCKED, false);
                y += 17;
                continue;
            }
            y = this.task(graphics, ranks, milestone, rank, accent, x, y);
        }
    }

    private int task(GuiGraphicsExtractor graphics, RingRanks.Ranks ranks, Milestone task, int rank, int accent, int x, int y) {
        boolean done = rank >= task.tier();
        boolean active = rank == task.tier() - 1;
        int progress = done ? task.goal() : active ? ranks.counter(task.key()) : 0;
        int color = done ? DONE : active ? LanternWidgets.TEXT : LOCKED;
        String glyph = done ? "✔" : active ? "▶" : "·";

        graphics.text(this.font, glyph, x, y, color, false);
        Component label = Component.translatable("gui.brightestday.spectrum.rank_short", task.tier()).append(" ")
                .append(Component.translatable(task.translationKey(), task.goal()));
        for (FormattedCharSequence line : this.font.split(label, DETAIL_WIDTH - 10)) {
            graphics.text(this.font, line, x + 9, y, color, false);
            y += 9;
        }

        int barWidth = DETAIL_WIDTH - 46;
        int filled = Math.round(barWidth * Math.min(1.0F, progress / (float) task.goal()));
        graphics.fill(x + 9, y + 1, x + 9 + barWidth, y + 1 + TASK_BAR_HEIGHT, LanternWidgets.BAR_EMPTY);
        graphics.fill(x + 9, y + 1, x + 9 + filled, y + 1 + TASK_BAR_HEIGHT, done ? DONE : accent);
        String count = progress + "/" + task.goal();
        graphics.text(this.font, count, x + DETAIL_WIDTH - this.font.width(count), y - 2, color, false);
        return y + 8;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            int x = this.left + MARGIN;
            Emotion[] emotions = Emotion.values();
            for (int i = 0; i < emotions.length; i++) {
                int y = this.rowY(i);
                if (event.x() < x || event.x() >= x + LIST_WIDTH || event.y() < y || event.y() >= y + ROW_HEIGHT - 2) continue;
                this.selected = emotions[i];
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
