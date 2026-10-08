package dev.amble.client.screens;

import dev.amble.BrightestDay;
import dev.amble.client.render.BatteryTextures;
import dev.amble.core.BrightestDayItems;
import dev.amble.core.official.OfficialServer;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OfficialServerScreen extends Screen {
    private static final Identifier GREY_STEVE = BrightestDay.id("dynamic/grey_steve");
    private static final int FACE = 20;
    private static final int BORDER = 2;
    private static final int FRAME = FACE + BORDER * 2;
    private static final int GAP = 4;
    private static final int STEP = FRAME + GAP;
    private static final int PADDING = 10;
    private static final int HEADER = 54;
    private static final int HINTS = 30;
    private static final int FOOTER = 36;
    private static final int MAX_COLUMNS = 12;
    private static final int BUTTON_WIDTH = 98;
    private static final int BUTTON_HEIGHT = 20;
    private static final int EMPTY_FRAME = 0xFF2A2A2E;
    private static final int EMPTY_TINT = 0xFF8C8C8C;
    private static final int NO_RING_FRAME = 0xFF5A5A60;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFA0A0A0;
    private static final int GOOD_PING = 150;
    private static final int OK_PING = 300;

    private static final ExecutorService PINGER = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "Brightest Day Official Server Pinger");
        thread.setDaemon(true);
        thread.setContextClassLoader(OfficialServerScreen.class.getClassLoader());
        return thread;
    });

    private enum State { PINGING, ONLINE, OFFLINE }

    private final @Nullable Screen parent;
    private final Map<UUID, ResolvableProfile> profiles = new HashMap<>();
    private ServerStatusPinger pinger = new ServerStatusPinger();
    private ServerData data = server();
    private volatile State state = State.PINGING;
    private boolean greySteve;
    private int scroll;
    private @Nullable Component parsedMotd;
    private ServerStatus.@Nullable Players parsedPlayers;
    private List<OfficialServer.Member> parsed = List.of();
    private boolean roster;

    public OfficialServerScreen(@Nullable Screen parent) {
        super(Component.literal(OfficialServer.NAME));
        this.parent = parent;
    }

    private static ServerData server() {
        ServerData data = new ServerData(OfficialServer.NAME, OfficialServer.IP, ServerData.Type.OTHER);
        data.ping = -1L;
        return data;
    }

    @Override
    protected void init() {
        if (!this.greySteve) {
            this.greySteve = BatteryTextures.load(this.minecraft.getResourceManager(), DefaultPlayerSkin.getDefaultTexture(), GREY_STEVE);
        }
        int y = this.height - FOOTER + (FOOTER - BUTTON_HEIGHT) / 2;
        int left = this.width / 2 - (BUTTON_WIDTH * 3 + 8) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.brightestday.official.join"), button -> this.join())
                .bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.brightestday.official.refresh"), button -> this.refresh())
                .bounds(left + BUTTON_WIDTH + 4, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> this.onClose())
                .bounds(left + (BUTTON_WIDTH + 4) * 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        if (this.state == State.PINGING && this.data.ping < 0) this.ping();
    }

    private void ping() {
        ServerData target = this.data;
        this.state = State.PINGING;
        PINGER.execute(() -> {
            try {
                this.pinger.pingServer(target, () -> {}, () -> {
                    if (target == this.data) this.state = State.ONLINE;
                }, EventLoopGroupHolder.remote(this.minecraft.options.useNativeTransport()));
            } catch (Exception | LinkageError exception) {
                if (target == this.data) this.state = State.OFFLINE;
            }
        });
    }

    private void refresh() {
        this.pinger.removeAll();
        this.pinger = new ServerStatusPinger();
        this.data = server();
        this.scroll = 0;
        this.ping();
    }

    private void join() {
        ServerData target = new ServerData(OfficialServer.NAME, OfficialServer.IP, ServerData.Type.OTHER);
        ConnectScreen.startConnecting(this, this.minecraft, ServerAddress.parseString(OfficialServer.IP), target, false, null);
    }

    @Override
    public void tick() {
        this.pinger.tick();
        if (this.state == State.PINGING && this.data.motd != null && this.data.motd.getContents() instanceof TranslatableContents contents
                && contents.getKey().equals("multiplayer.status.cannot_connect")) {
            this.state = State.OFFLINE;
        }
    }

    private List<OfficialServer.Member> members() {
        if (this.data.motd == this.parsedMotd && this.data.players == this.parsedPlayers) return this.parsed;
        this.parsedMotd = this.data.motd;
        this.parsedPlayers = this.data.players;
        this.parsed = this.parse();
        return this.parsed;
    }

    private List<OfficialServer.Member> parse() {
        if (this.data.motd != null) {
            Optional<List<OfficialServer.Member>> roster = OfficialServer.read(this.data.motd);
            this.roster = roster.isPresent();
            if (roster.isPresent()) return roster.get();
        }
        List<OfficialServer.Member> members = new ArrayList<>();
        ServerStatus.Players players = this.data.players;
        if (players != null) {
            players.sample().forEach(player -> members.add(new OfficialServer.Member(player.id(), player.name(), Optional.empty(), 0)));
        }
        return members;
    }

    private int slots(List<OfficialServer.Member> members) {
        ServerStatus.Players players = this.data.players;
        int max = players == null ? 0 : players.max();
        return Math.max(max, members.size());
    }

    private int columns(int slots) {
        int fit = Math.max(1, (this.width - 40 - PADDING * 2) / STEP);
        return Mth.clamp(Math.min(slots, MAX_COLUMNS), 1, fit);
    }

    private int visibleRows() {
        return Math.max(1, (this.height - FOOTER - HEADER - HINTS - PADDING * 2 - 8) / STEP);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        int slots = this.slots(this.members());
        int rows = Mth.positiveCeilDiv(Math.max(1, slots), this.columns(slots));
        this.scroll = Mth.clamp(this.scroll - (int) Math.signum(scrollY), 0, Math.max(0, rows - this.visibleRows()));
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int center = this.width / 2;
        List<OfficialServer.Member> members = this.members();
        int slots = this.slots(members);
        int columns = this.columns(Math.max(1, slots));
        int rows = slots == 0 ? 0 : Math.min(Mth.positiveCeilDiv(slots, columns), this.visibleRows());
        int boxHeight = slots == 0 ? 20 : rows * STEP - GAP + PADDING * 2;
        int top = Math.max(8, (this.height - FOOTER - HEADER - boxHeight - HINTS) / 2);
        int gridTop = top + HEADER;

        graphics.centeredText(this.font, this.title, center, top, TEXT);
        graphics.centeredText(this.font, Component.translatable("gui.brightestday.official.ip", OfficialServer.IP), center, top + 14, TEXT_DIM);
        graphics.centeredText(this.font, this.statusLine(), center, top + 26, TEXT);

        if (slots == 0) {
            if (this.state != State.PINGING) graphics.centeredText(this.font, Component.translatable("gui.brightestday.official.nobody"), center, gridTop + 6, TEXT_DIM);
            return;
        }

        int boxWidth = columns * STEP - GAP + PADDING * 2;
        int boxLeft = center - boxWidth / 2;
        LanternWidgets.panel(graphics, boxLeft, gridTop, boxWidth, boxHeight, this.accent(members));

        int first = this.scroll * columns;
        int last = Math.min(slots, first + rows * columns);
        OfficialServer.Member hovered = null;
        for (int slot = first; slot < last; slot++) {
            int x = boxLeft + PADDING + (slot % columns) * STEP;
            int y = gridTop + PADDING + (slot / columns - this.scroll) * STEP;
            if (slot >= members.size()) {
                this.empty(graphics, x, y);
                continue;
            }
            OfficialServer.Member member = members.get(slot);
            boolean over = mouseX >= x && mouseY >= y && mouseX < x + FRAME && mouseY < y + FRAME;
            this.member(graphics, member, x, y, over);
            if (over) hovered = member;
        }

        int total = Mth.positiveCeilDiv(slots, columns);
        if (total > rows) {
            graphics.centeredText(this.font, Component.translatable("gui.brightestday.official.scroll", this.scroll + 1, total - rows + 1), center, gridTop + boxHeight + 4, TEXT_DIM);
        }
        if (!this.roster && !members.isEmpty()) {
            graphics.centeredText(this.font, Component.translatable("gui.brightestday.official.no_roster"), center, gridTop + boxHeight + (total > rows ? 16 : 4), TEXT_DIM);
        }
        if (hovered != null) graphics.setTooltipForNextFrame(this.font, this.tooltip(hovered), Optional.empty(), mouseX, mouseY);
    }

    private int accent(List<OfficialServer.Member> members) {
        for (OfficialServer.Member member : members) {
            if (member.corps().isPresent()) return ARGB.opaque(member.color());
        }
        return LanternWidgets.accent();
    }

    private List<Component> tooltip(OfficialServer.Member member) {
        List<Component> lines = new ArrayList<>();
        if (member.corps().isPresent()) {
            lines.add(Component.literal(member.name()).withColor(ARGB.opaque(member.color())));
            lines.add(member.corps().get().displayName().copy().withColor(ARGB.opaque(member.color())));
        } else {
            lines.add(Component.literal(member.name()).withColor(TEXT));
            if (this.roster) lines.add(Component.translatable("gui.brightestday.no_ring").withColor(TEXT_DIM));
        }
        return lines;
    }

    private Component statusLine() {
        if (this.state == State.OFFLINE) return Component.translatable("gui.brightestday.official.offline").withColor(0xFFFF5555);
        if (this.data.ping < 0) return Component.translatable("gui.brightestday.official.pinging").withColor(TEXT_DIM);
        int ping = (int) this.data.ping;
        int color = ping < GOOD_PING ? 0xFF55FF55 : ping < OK_PING ? 0xFFFFFF55 : 0xFFFF5555;
        ServerStatus.Players players = this.data.players;
        Component count = players == null ? Component.literal("?") : Component.literal(players.online() + " / " + players.max());
        Component line = Component.translatable("gui.brightestday.official.ping", Component.literal(ping + " ms").withColor(color))
                .append("   ").append(Component.translatable("gui.brightestday.official.players", count));
        if (this.data.protocol != SharedConstants.getCurrentVersion().protocolVersion()) {
            line = line.copy().append("   ").append(Component.translatable("gui.brightestday.official.version", this.data.version).withColor(0xFFFF5555));
        }
        return line;
    }

    private void member(GuiGraphicsExtractor graphics, OfficialServer.Member member, int x, int y, boolean hovered) {
        boolean ringed = member.corps().isPresent();
        int color = ringed ? ARGB.opaque(member.color()) : NO_RING_FRAME;
        graphics.fill(x, y, x + FRAME, y + FRAME, hovered ? ARGB.srgbLerp(0.5F, color, 0xFFFFFFFF) : color);
        ResolvableProfile profile = this.profiles.computeIfAbsent(member.id(), ResolvableProfile::createUnresolved);
        PlayerFaceExtractor.extractRenderState(graphics, profile, x + BORDER, y + BORDER, FACE);
        member.corps().ifPresent(corps -> {
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + FRAME - 11.0F, y + FRAME - 11.0F);
            graphics.pose().scale(0.75F, 0.75F);
            graphics.item(new ItemStack(BrightestDayItems.ring(corps)), 0, 0);
            graphics.pose().popMatrix();
        });
    }

    private void empty(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + FRAME, y + FRAME, EMPTY_FRAME);
        Identifier texture = this.greySteve ? GREY_STEVE : DefaultPlayerSkin.getDefaultTexture();
        PlayerFaceExtractor.extractRenderState(graphics, texture, x + BORDER, y + BORDER, FACE, true, false, this.greySteve ? EMPTY_TINT : 0xFF555555);
    }

    @Override
    public void removed() {
        this.pinger.removeAll();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}
