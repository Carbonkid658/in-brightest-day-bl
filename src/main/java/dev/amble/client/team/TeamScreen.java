package dev.amble.client.team;

import dev.amble.client.screens.LanternWidgets;
import dev.amble.config.BrightestDayConfig;
import dev.amble.core.BrightestDayItems;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.networking.payloads.c2s.TeamActionC2SPayload;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.team.EmotionalSpectrum;
import dev.amble.core.team.LanternTeams;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TeamScreen extends Screen {
    private static final int WIDTH = 248;
    private static final int HEIGHT = 232;
    private static final int MARGIN = 12;
    private static final int SPECTRUM_Y = 32;
    private static final int SPECTRUM_HEIGHT = 5;
    private static final int CAP_WIDTH = 10;
    private static final int CAP_GAP = 3;
    private static final int TEAM_Y = 58;
    private static final int CHIP_Y = 72;
    private static final int NEARBY_Y = 96;
    private static final int ROWS_Y = 108;
    private static final int ROW_HEIGHT = 22;
    private static final int VISIBLE_ROWS = 4;
    private static final int BUTTON_HEIGHT = 14;
    private static final int BUTTON_WIDTH = 46;
    private static final int SMALL_BUTTON = 14;
    private static final int LIST_RANGE_SCALE = 2;
    private static final int TEAMMATE = 0xFF7DFF9A;
    private static final int TOO_FAR = 0xFF7A8A7E;

    private record Lantern(Player player, LanternCorps corps, double distance, boolean incoming, boolean outgoing, boolean inRange) {}

    private record Hit(int x, int y, int width, int height, Runnable action) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
        }
    }

    private final @Nullable Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private int scroll;
    private int left;
    private int top;

    public TeamScreen(@Nullable Screen parent) {
        super(Component.translatable("gui.brightestday.team.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.left = (this.width - WIDTH) / 2;
        this.top = (this.height - HEIGHT) / 2;
    }

    private Optional<LanternCorps> ownCorps() {
        return PowerRingItem.getWornCorps(this.minecraft.player);
    }

    private List<Lantern> nearby() {
        Player self = this.minecraft.player;
        double inviteRange = BrightestDayConfig.get().teamInviteRange;
        Optional<LanternCorps> mine = this.ownCorps();
        List<Lantern> lanterns = new ArrayList<>();
        for (AbstractClientPlayer other : this.minecraft.level.players()) {
            if (other == self || other.isSpectator() || LanternTeams.areTeammates(self, other)) continue;
            Optional<LanternCorps> corps = PowerRingItem.getWornCorps(other);
            if (corps.isEmpty()) continue;

            double distance = self.distanceTo(other);
            boolean incoming = ClientTeams.invitedBy(other.getUUID());
            if (distance > inviteRange * LIST_RANGE_SCALE && !incoming) continue;
            lanterns.add(new Lantern(other, corps.get(), distance, incoming, ClientTeams.invited(other.getUUID()), distance <= inviteRange));
        }

        Comparator<Lantern> order = Comparator.comparing((Lantern lantern) -> !lantern.incoming());
        if (mine.isPresent()) order = order.thenComparingInt(lantern -> EmotionalSpectrum.distance(mine.get(), lantern.corps()));
        lanterns.sort(order.thenComparingDouble(Lantern::distance));
        return lanterns;
    }

    private List<Player> teammates() {
        List<Player> members = new ArrayList<>();
        for (AbstractClientPlayer other : this.minecraft.level.players()) {
            if (LanternTeams.areTeammates(this.minecraft.player, other)) members.add(other);
        }
        members.sort(Comparator.comparing(player -> player.getName().getString()));
        return members;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.hits.clear();
        if (this.minecraft.player == null || this.minecraft.level == null) return;

        LanternWidgets.panel(graphics, this.left, this.top, WIDTH, HEIGHT, LanternWidgets.accent());
        graphics.text(this.font, this.title, this.left + MARGIN, this.top + 11, LanternWidgets.TEXT, true);
        this.ownCorps().ifPresent(corps -> {
            Component name = corps.displayName();
            graphics.text(this.font, name, this.left + WIDTH - MARGIN - this.font.width(name), this.top + 11, ARGB.opaque(corps.color()), true);
        });

        List<Lantern> lanterns = this.nearby();
        List<Player> teammates = this.teammates();
        this.spectrum(graphics, lanterns, teammates);
        this.team(graphics, teammates, mouseX, mouseY);
        this.list(graphics, lanterns, mouseX, mouseY);

        List<FormattedCharSequence> hint = this.font.split(Component.translatable("gui.brightestday.team.hint"), WIDTH - MARGIN * 2);
        int hintY = this.top + HEIGHT - 10 - hint.size() * 10;
        for (FormattedCharSequence line : hint) {
            graphics.text(this.font, line, this.left + MARGIN, hintY, LanternWidgets.TEXT_DIM, false);
            hintY += 10;
        }
    }

    private void spectrum(GuiGraphicsExtractor graphics, List<Lantern> lanterns, List<Player> teammates) {
        int x0 = this.left + MARGIN;
        int x1 = this.left + WIDTH - MARGIN;
        int y = this.top + SPECTRUM_Y;
        int innerLeft = x0 + CAP_WIDTH + CAP_GAP;
        int innerRight = x1 - CAP_WIDTH - CAP_GAP;

        graphics.fill(x0 - 1, y - 1, x0 + CAP_WIDTH + 1, y + SPECTRUM_HEIGHT + 1, LanternWidgets.BAR_FRAME);
        graphics.fill(x0, y, x0 + CAP_WIDTH, y + SPECTRUM_HEIGHT, ARGB.opaque(LanternCorps.BLACK.color()));
        graphics.fill(x1 - CAP_WIDTH - 1, y - 1, x1 + 1, y + SPECTRUM_HEIGHT + 1, LanternWidgets.BAR_FRAME);
        graphics.fill(x1 - CAP_WIDTH, y, x1, y + SPECTRUM_HEIGHT, ARGB.opaque(LanternCorps.WHITE.color()));
        graphics.fill(innerLeft - 1, y - 1, innerRight + 1, y + SPECTRUM_HEIGHT + 1, LanternWidgets.BAR_FRAME);

        int last = EmotionalSpectrum.VISIBLE.size() - 1;
        for (int x = innerLeft; x < innerRight; x++) {
            float position = (x - innerLeft) / (float) (innerRight - innerLeft - 1) * last;
            int from = Mth.clamp((int) Math.floor(position), 0, last);
            int to = Math.min(from + 1, last);
            int color = ARGB.srgbLerp(position - from, ARGB.opaque(EmotionalSpectrum.VISIBLE.get(from).color()), ARGB.opaque(EmotionalSpectrum.VISIBLE.get(to).color()));
            graphics.fill(x, y, x + 1, y + SPECTRUM_HEIGHT, color);
        }

        Map<LanternCorps, Integer> stacks = new EnumMap<>(LanternCorps.class);
        for (Player teammate : teammates) {
            PowerRingItem.getWornCorps(teammate).ifPresent(corps -> this.marker(graphics, corps, stacks, TEAMMATE, innerLeft, innerRight, x0, x1, y));
        }
        for (Lantern lantern : lanterns) {
            this.marker(graphics, lantern.corps(), stacks, ARGB.opaque(lantern.corps().color()), innerLeft, innerRight, x0, x1, y);
        }

        this.ownCorps().ifPresent(corps -> {
            int cx = markerX(corps, innerLeft, innerRight, x0, x1);
            for (int row = 0; row < 3; row++) graphics.fill(cx - row, y + SPECTRUM_HEIGHT + 2 + row, cx + row + 1, y + SPECTRUM_HEIGHT + 3 + row, 0xFFFFFFFF);
            Component you = Component.translatable("gui.brightestday.team.you");
            graphics.text(this.font, you, Mth.clamp(cx - this.font.width(you) / 2, x0, x1 - this.font.width(you)), y + SPECTRUM_HEIGHT + 6, LanternWidgets.TEXT, false);
        });
    }

    private void marker(GuiGraphicsExtractor graphics, LanternCorps corps, Map<LanternCorps, Integer> stacks, int color,
                        int innerLeft, int innerRight, int x0, int x1, int y) {
        int stack = stacks.merge(corps, 1, Integer::sum) - 1;
        int cx = markerX(corps, innerLeft, innerRight, x0, x1);
        int my = y - 4 - stack * 4;
        graphics.fill(cx - 2, my - 1, cx + 3, my + 3, 0xFF06140B);
        graphics.fill(cx - 1, my, cx + 2, my + 2, color);
    }

    private static int markerX(LanternCorps corps, int innerLeft, int innerRight, int x0, int x1) {
        if (corps == LanternCorps.BLACK) return x0 + CAP_WIDTH / 2;
        if (corps == LanternCorps.WHITE) return x1 - CAP_WIDTH / 2;
        int last = EmotionalSpectrum.VISIBLE.size() - 1;
        return innerLeft + Math.round(EmotionalSpectrum.position(corps) / (float) last * (innerRight - innerLeft - 1));
    }

    private void team(GuiGraphicsExtractor graphics, List<Player> teammates, int mouseX, int mouseY) {
        int x0 = this.left + MARGIN;
        int y = this.top + TEAM_Y;
        graphics.text(this.font, Component.translatable("gui.brightestday.team.your_team"), x0, y, LanternWidgets.TEXT, true);
        divider(graphics, x0 + this.font.width(Component.translatable("gui.brightestday.team.your_team")) + 6, this.left + WIDTH - MARGIN - BUTTON_WIDTH - 6, y + 4);

        if (LanternTeams.team(this.minecraft.player).isPresent()) {
            UUID self = this.minecraft.player.getUUID();
            this.button(graphics, this.left + WIDTH - MARGIN - BUTTON_WIDTH, y - 3, BUTTON_WIDTH, Component.translatable("gui.brightestday.team.leave"),
                    mouseX, mouseY, () -> ClientTeams.send(TeamActionC2SPayload.Action.LEAVE, self));
        }

        int chipY = this.top + CHIP_Y;
        if (teammates.isEmpty()) {
            Component hint = this.ownCorps().isPresent() ? Component.translatable("gui.brightestday.team.solo") : Component.translatable("gui.brightestday.team.no_ring");
            graphics.text(this.font, hint, x0, chipY + 4, LanternWidgets.TEXT_DIM, false);
            return;
        }

        int chipX = x0;
        int limit = this.left + WIDTH - MARGIN;
        for (int i = 0; i < teammates.size(); i++) {
            Player teammate = teammates.get(i);
            Component name = teammate.getName();
            int chipWidth = 20 + this.font.width(name) + 6;
            if (chipX + chipWidth > limit) {
                graphics.text(this.font, "+" + (teammates.size() - i), chipX, chipY + 4, LanternWidgets.TEXT_DIM, false);
                break;
            }
            Optional<LanternCorps> corps = PowerRingItem.getWornCorps(teammate);
            int color = corps.map(c -> ARGB.opaque(c.color())).orElse(TEAMMATE);
            graphics.fill(chipX, chipY - 1, chipX + chipWidth, chipY + 17, ARGB.color(0x50, color));
            graphics.fill(chipX, chipY - 1, chipX + 2, chipY + 17, color);
            if (corps.isPresent()) graphics.item(new ItemStack(BrightestDayItems.ring(corps.get())), chipX + 3, chipY);
            graphics.text(this.font, name, chipX + 21, chipY + 4, 0xFFFFFFFF, true);
            chipX += chipWidth + 4;
        }
    }

    private void list(GuiGraphicsExtractor graphics, List<Lantern> lanterns, int mouseX, int mouseY) {
        int x0 = this.left + MARGIN;
        int x1 = this.left + WIDTH - MARGIN;
        int y = this.top + NEARBY_Y;
        Component header = Component.translatable("gui.brightestday.team.nearby");
        graphics.text(this.font, header, x0, y, LanternWidgets.TEXT, true);
        divider(graphics, x0 + this.font.width(header) + 6, x1, y + 4);

        int rowsTop = this.top + ROWS_Y;
        if (lanterns.isEmpty()) {
            graphics.text(this.font, Component.translatable("gui.brightestday.team.none", (int) BrightestDayConfig.get().teamInviteRange),
                    x0, rowsTop + 6, LanternWidgets.TEXT_DIM, false);
            return;
        }

        this.scroll = Mth.clamp(this.scroll, 0, Math.max(lanterns.size() - VISIBLE_ROWS, 0));
        Optional<LanternCorps> mine = this.ownCorps();
        int rowRight = lanterns.size() > VISIBLE_ROWS ? x1 - 4 : x1;
        for (int i = 0; i < VISIBLE_ROWS && i + this.scroll < lanterns.size(); i++) {
            this.row(graphics, lanterns.get(i + this.scroll), mine, x0, rowRight, rowsTop + i * ROW_HEIGHT, mouseX, mouseY);
        }

        if (lanterns.size() > VISIBLE_ROWS) {
            int trackHeight = VISIBLE_ROWS * ROW_HEIGHT - 2;
            int thumbHeight = Math.max(8, trackHeight * VISIBLE_ROWS / lanterns.size());
            int thumbY = rowsTop + (trackHeight - thumbHeight) * this.scroll / (lanterns.size() - VISIBLE_ROWS);
            graphics.fill(x1 - 2, rowsTop, x1, rowsTop + trackHeight, LanternWidgets.BAR_EMPTY);
            graphics.fill(x1 - 2, thumbY, x1, thumbY + thumbHeight, LanternWidgets.BAR_FRAME);
        }
    }

    private void row(GuiGraphicsExtractor graphics, Lantern lantern, Optional<LanternCorps> mine, int x0, int x1, int y, int mouseX, int mouseY) {
        int color = ARGB.opaque(lantern.corps().color());
        boolean hovered = mouseX >= x0 && mouseX < x1 && mouseY >= y && mouseY < y + ROW_HEIGHT - 2;
        graphics.fill(x0, y, x1, y + ROW_HEIGHT - 2, hovered ? ARGB.color(0x48, color) : 0x30000000);
        graphics.fill(x0, y, x0 + 2, y + ROW_HEIGHT - 2, color);
        graphics.item(new ItemStack(BrightestDayItems.ring(lantern.corps())), x0 + 4, y + 2);

        int actionsLeft = this.actions(graphics, lantern, x1 - 3, y + (ROW_HEIGHT - 2 - BUTTON_HEIGHT) / 2, mouseX, mouseY);
        Component distance = Component.translatable("gui.brightestday.team.distance", Math.round(lantern.distance()));
        int distanceX = actionsLeft - 4 - this.font.width(distance);
        graphics.text(this.font, distance, distanceX, y + 7, lantern.inRange() ? LanternWidgets.TEXT_DIM : TOO_FAR, false);

        int textX = x0 + 24;
        int textWidth = distanceX - textX - 4;
        String name = lantern.player().getName().getString();
        if (this.font.width(name) > textWidth) name = this.font.plainSubstrByWidth(name, textWidth - this.font.width("…")) + "…";
        graphics.text(this.font, name, textX, y + 2, 0xFFFFFFFF, true);

        Component detail = lantern.corps().displayName().copy().withColor(color);
        if (mine.isPresent()) {
            EmotionalSpectrum.Affinity affinity = EmotionalSpectrum.affinity(mine.get(), lantern.corps());
            detail = Component.empty().append(detail).append(Component.literal(" · ").withColor(LanternWidgets.TEXT_DIM))
                    .append(Component.translatable(affinity.translationKey()).withColor(LanternWidgets.TEXT_DIM));
        }
        String plain = detail.getString();
        if (this.font.width(plain) > textWidth) {
            graphics.text(this.font, this.font.plainSubstrByWidth(plain, textWidth - this.font.width("…")) + "…", textX, y + 11, color, false);
        } else {
            graphics.text(this.font, detail, textX, y + 11, 0xFFFFFFFF, false);
        }
    }

    private int actions(GuiGraphicsExtractor graphics, Lantern lantern, int right, int y, int mouseX, int mouseY) {
        UUID target = lantern.player().getUUID();
        if (lantern.incoming()) {
            int declineX = right - SMALL_BUTTON;
            int acceptX = declineX - 2 - BUTTON_WIDTH;
            this.button(graphics, declineX, y, SMALL_BUTTON, Component.literal("✕"), mouseX, mouseY,
                    () -> ClientTeams.send(TeamActionC2SPayload.Action.DECLINE, target));
            this.button(graphics, acceptX, y, BUTTON_WIDTH, Component.translatable("gui.brightestday.team.accept"), mouseX, mouseY,
                    () -> ClientTeams.send(TeamActionC2SPayload.Action.ACCEPT, target));
            return acceptX;
        }
        if (this.ownCorps().isEmpty()) return right;
        if (!lantern.inRange()) {
            Component far = Component.translatable("gui.brightestday.team.too_far");
            graphics.text(this.font, far, right - this.font.width(far), y + 3, TOO_FAR, false);
            return right - this.font.width(far);
        }

        int x = right - BUTTON_WIDTH;
        if (lantern.outgoing()) {
            this.button(graphics, x, y, BUTTON_WIDTH, Component.translatable("gui.brightestday.team.pending"), mouseX, mouseY,
                    () -> ClientTeams.send(TeamActionC2SPayload.Action.CANCEL, target));
        } else {
            this.button(graphics, x, y, BUTTON_WIDTH, Component.translatable("gui.brightestday.team.invite"), mouseX, mouseY,
                    () -> ClientTeams.send(TeamActionC2SPayload.Action.INVITE, target));
        }
        return x;
    }

    private void button(GuiGraphicsExtractor graphics, int x, int y, int width, Component label, int mouseX, int mouseY, Runnable action) {
        Hit hit = new Hit(x, y, width, BUTTON_HEIGHT, action);
        boolean hovered = hit.contains(mouseX, mouseY);
        LanternWidgets.button(graphics, x, y, width, BUTTON_HEIGHT, LanternWidgets.accent(), true, hovered, 1.0F);
        graphics.text(this.font, label, x + (width - this.font.width(label)) / 2, y + 3, hovered ? 0xFFFFFFFF : LanternWidgets.TEXT, true);
        this.hits.add(hit);
    }

    private static void divider(GuiGraphicsExtractor graphics, int x0, int x1, int y) {
        if (x1 > x0) graphics.fill(x0, y, x1, y + 1, ARGB.color(0x80, LanternWidgets.BAR_FRAME));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            for (Hit hit : List.copyOf(this.hits)) {
                if (!hit.contains(event.x(), event.y())) continue;
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                hit.action().run();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        this.scroll -= (int) Math.signum(scrollY);
        return true;
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
