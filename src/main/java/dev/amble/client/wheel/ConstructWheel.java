package dev.amble.client.wheel;

import dev.amble.BrightestDay;
import dev.amble.client.BrightestDayKeybinds;
import dev.amble.client.effects.ConstructClient;
import dev.amble.client.effects.SculptClient;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.networking.payloads.c2s.SelectConstructC2SPayload;
import dev.amble.core.ringpowers.CorpsColors;
import dev.amble.core.ringpowers.RingPowerRegistry;
import dev.amble.core.ringpowers.constructs.ConstructRingPower;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import dev.amble.core.sculpt.SculptShape;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * The hold-R construct picker: a semicircle on the left edge steered by the mouse while the camera
 * is frozen. Pushing past the rim of a slice with sub-modes latches it and fans out a sub-arc.
 */
public final class ConstructWheel {
    private static final int INNER = 30;
    private static final int OUTER = 92;
    private static final int SUB_INNER = 98;
    private static final int SUB_OUTER = 146;
    // pulling the cursor back inside this radius unlatches the sub-arc
    private static final int SUB_LATCH = SUB_INNER - 4;
    private static final double SUB_SLICE_DEGREES = 32.0;
    private static final double GAP_DEGREES = 1.2;
    private static final int CELL = 2;
    private static final double MAX_CURSOR = SUB_OUTER + 10.0;
    private static final double MOUSE_SCALE = 0.5;
    private static final int TAP_TICKS = 5;
    private static final double TAP_DISTANCE = 3.0;
    private static final int HUD_MARGIN = 4;
    private static final int HUD_PADDING = 4;

    // items, not stacks: an ItemStack can't be built until item components are bound, well after client init
    private static final Map<ConstructRingPower, Item> ICONS = Map.of(
            RingPowerRegistry.BLAST, Items.FIRE_CHARGE,
            RingPowerRegistry.BEAM, Items.END_ROD,
            RingPowerRegistry.HEAL_BEAM, Items.GLISTERING_MELON_SLICE,
            RingPowerRegistry.ENTITY_SHIELD, Items.HEART_OF_THE_SEA,
            RingPowerRegistry.AREA_SHIELD, Items.TURTLE_HELMET,
            RingPowerRegistry.WALL, Items.STONE_BRICKS,
            RingPowerRegistry.SCULPT, Items.AMETHYST_SHARD,
            RingPowerRegistry.TOOL_FORGE, Items.ANVIL
    );
    private static final Map<SculptShape, Item> SHAPE_ICONS = Map.of(
            SculptShape.FREEFORM, Items.FEATHER,
            SculptShape.STAIRS, Items.OAK_STAIRS,
            SculptShape.TUBE, Items.HOPPER,
            SculptShape.CAGE, Items.IRON_BARS
    );
    private static final Item SHIELD_ICON = Items.SHIELD;
    private static final Item FALLBACK_ICON = Items.NETHER_STAR;
    private static final Map<Item, ItemStack> STACKS = new HashMap<>();

    private record Sub(Component name, Item icon, Runnable apply, BooleanSupplier current) {}

    private record Entry(Component name, Item icon, Set<ConstructRingPower> constructs, Runnable apply, List<Sub> subs) {
        boolean isCurrent(Player player) {
            return ArmedRingPower.selectedConstruct(player).map(this.constructs::contains).orElse(false);
        }

        Optional<Sub> currentSub() {
            return this.subs.stream().filter(sub -> sub.current().getAsBoolean()).findFirst();
        }
    }

    private static List<Entry> entries = List.of();
    private static boolean open;
    private static int heldTicks;
    private static double cursorX;
    private static double cursorY;
    private static double travelled;
    private static int hoveredEntry = -1;
    private static int hoveredSub = -1;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(ConstructWheel::tick);
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, BrightestDay.id("construct_wheel"), ConstructWheel::extractWheel);
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, BrightestDay.id("construct_indicator"), ConstructWheel::extractIndicator);
    }

    public static boolean isOpen() {
        return open;
    }

    /** Takes over mouse movement while the wheel is open; returns true when the camera shouldn't turn. */
    public static boolean onMouse(double dx, double dy) {
        if (!open) return false;

        double scale = MOUSE_SCALE / Minecraft.getInstance().getWindow().getGuiScale();
        cursorX = Math.max(cursorX + dx * scale, 1.0);
        cursorY += dy * scale;
        travelled += Math.hypot(dx * scale, dy * scale);
        double length = Math.hypot(cursorX, cursorY);
        if (length > MAX_CURSOR) {
            cursorX *= MAX_CURSOR / length;
            cursorY *= MAX_CURSOR / length;
        }
        updateHover();
        return true;
    }

    private static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        entries = player == null ? List.of() : build(player);

        boolean pressed = false;
        while (BrightestDayKeybinds.CYCLE_CONSTRUCT.consumeClick()) pressed = true;
        boolean down = player != null && client.gui.screen() == null && BrightestDayKeybinds.CYCLE_CONSTRUCT.isDown();

        if (!open) {
            if (!pressed && !down) return;
            // with nothing to pick, fall back to the server's cycle so it can explain why
            if (entries.isEmpty()) {
                ConstructClient.cycle();
                return;
            }
            if (!down) {
                ConstructClient.cycle();
                return;
            }
            openWheel(player);
            return;
        }

        if (down && !entries.isEmpty()) {
            heldTicks++;
            return;
        }
        open = false;
        if (heldTicks <= TAP_TICKS && travelled < TAP_DISTANCE) {
            ConstructClient.cycle();
        } else if (hoveredEntry >= 0 && hoveredEntry < entries.size()) {
            Entry entry = entries.get(hoveredEntry);
            if (hoveredSub >= 0 && hoveredSub < entry.subs().size()) {
                entry.subs().get(hoveredSub).apply().run();
            } else {
                entry.apply().run();
            }
        }
    }

    private static void openWheel(LocalPlayer player) {
        open = true;
        heldTicks = 0;
        travelled = 0.0;
        hoveredSub = -1;
        hoveredEntry = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).isCurrent(player)) hoveredEntry = i;
        }
        double angle = Math.toRadians(sliceCenter(hoveredEntry, entries.size()));
        double radius = (INNER + OUTER) / 2.0;
        cursorX = Math.cos(angle) * radius;
        cursorY = Math.sin(angle) * radius;
    }

    private static void updateHover() {
        if (entries.isEmpty()) return;
        double radius = Math.hypot(cursorX, cursorY);
        double angle = Math.toDegrees(Math.atan2(cursorY, cursorX));

        boolean latched = hoveredEntry >= 0 && hoveredEntry < entries.size() && !entries.get(hoveredEntry).subs().isEmpty() && radius >= SUB_LATCH;
        if (latched) {
            Entry entry = entries.get(hoveredEntry);
            double start = subStart(hoveredEntry, entry.subs().size());
            hoveredSub = Mth.clamp((int) Math.floor((angle - start) / SUB_SLICE_DEGREES), 0, entry.subs().size() - 1);
        } else {
            hoveredEntry = sliceAt(angle, entries.size());
            hoveredSub = -1;
        }
    }

    private static List<Entry> build(Player player) {
        List<ConstructRingPower> owned = ArmedRingPower.constructs(player);
        List<Entry> built = new ArrayList<>();
        List<Sub> shieldSubs = new ArrayList<>();
        int shieldIndex = -1;

        for (ConstructRingPower construct : owned) {
            if (construct == RingPowerRegistry.ENTITY_SHIELD || construct == RingPowerRegistry.AREA_SHIELD) {
                if (shieldIndex < 0) {
                    shieldIndex = built.size();
                    built.add(null);
                }
                shieldSubs.add(new Sub(Component.translatable(construct.getTranslationKey()), icon(construct),
                        () -> select(player, construct), () -> isSelected(player, construct)));
            } else if (construct == RingPowerRegistry.SCULPT) {
                List<Sub> shapes = new ArrayList<>();
                for (SculptShape shape : SculptShape.values()) {
                    shapes.add(new Sub(Component.translatable(shape.translationKey()), SHAPE_ICONS.get(shape), () -> {
                        SculptClient.setShape(shape);
                        select(player, construct);
                    }, () -> SculptClient.shape() == shape));
                }
                built.add(new Entry(Component.translatable(construct.getTranslationKey()), icon(construct), Set.of(construct),
                        () -> select(player, construct), shapes));
            } else {
                built.add(new Entry(Component.translatable(construct.getTranslationKey()), icon(construct), Set.of(construct),
                        () -> select(player, construct), List.of()));
            }
        }

        if (shieldIndex >= 0) {
            Set<ConstructRingPower> shields = Set.copyOf(owned.stream()
                    .filter(construct -> construct == RingPowerRegistry.ENTITY_SHIELD || construct == RingPowerRegistry.AREA_SHIELD)
                    .toList());
            Sub first = shieldSubs.getFirst();
            built.set(shieldIndex, new Entry(Component.translatable("construct_group.brightestday.shield"), SHIELD_ICON, shields, () -> {
                boolean alreadyShield = ArmedRingPower.selectedConstruct(player).map(shields::contains).orElse(false);
                if (!alreadyShield) first.apply().run();
            }, List.copyOf(shieldSubs)));
        }
        return built;
    }

    private static ItemStack stack(Item item) {
        return STACKS.computeIfAbsent(item, ItemStack::new);
    }

    private static Item icon(ConstructRingPower construct) {
        return ICONS.getOrDefault(construct, FALLBACK_ICON);
    }

    private static boolean isSelected(Player player, ConstructRingPower construct) {
        return ArmedRingPower.selectedConstruct(player).orElse(null) == construct;
    }

    private static void select(Player player, ConstructRingPower construct) {
        if (!isSelected(player, construct)) ClientPlayNetworking.send(new SelectConstructC2SPayload(construct.id()));
    }

    private static double sliceWidth(int count) {
        return 180.0 / Math.max(count, 1);
    }

    private static double sliceCenter(int index, int count) {
        return -90.0 + (index + 0.5) * sliceWidth(count);
    }

    private static int sliceAt(double angle, int count) {
        return Mth.clamp((int) Math.floor((angle + 90.0) / sliceWidth(count)), 0, count - 1);
    }

    /** Where a latched entry's sub-arc begins: centred on the entry, but kept on screen. */
    private static double subStart(int index, int subs) {
        double span = subs * SUB_SLICE_DEGREES;
        return Mth.clamp(sliceCenter(index, entries.size()) - span / 2.0, -90.0, Math.max(90.0 - span, -90.0));
    }

    private static int darken(int color, float factor) {
        return ARGB.color(255, Math.round(ARGB.red(color) * factor), Math.round(ARGB.green(color) * factor), Math.round(ARGB.blue(color) * factor));
    }

    private static int sliceColor(int color, boolean hovered, boolean current) {
        if (hovered) return ARGB.color(0xB0, color);
        if (current) return ARGB.color(0x90, darken(color, 0.6F));
        return ARGB.color(0x70, darken(color, 0.3F));
    }

    private static void extractWheel(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (!open || player == null || entries.isEmpty()) return;

        int color = ARGB.opaque(CorpsColors.of(player));
        int centerY = graphics.guiHeight() / 2;
        int count = entries.size();
        Entry focused = hoveredEntry >= 0 && hoveredEntry < count ? entries.get(hoveredEntry) : null;
        double subStart = focused == null || focused.subs().isEmpty() ? 0.0 : subStart(hoveredEntry, focused.subs().size());
        boolean[] currentMain = new boolean[count];
        for (int i = 0; i < count; i++) currentMain[i] = entries.get(i).isCurrent(player);
        boolean[] currentSub = new boolean[focused == null ? 0 : focused.subs().size()];
        for (int j = 0; j < currentSub.length; j++) currentSub[j] = focused.subs().get(j).current().getAsBoolean();

        // the rings are painted as merged horizontal runs of small cells, sliced by angle
        for (int y = -SUB_OUTER; y < SUB_OUTER; y += CELL) {
            int runStart = 0;
            int runColor = 0;
            for (int x = 0; x <= SUB_OUTER; x += CELL) {
                int cellColor = x == SUB_OUTER ? 0 : cellColor(color, x + CELL * 0.5, y + CELL * 0.5, count, focused, subStart, currentMain, currentSub);
                if (cellColor != runColor) {
                    if (runColor != 0) graphics.fill(runStart, centerY + y, x, centerY + y + CELL, runColor);
                    runStart = x;
                    runColor = cellColor;
                }
            }
        }

        for (int i = 0; i < count; i++) {
            drawIcon(graphics, entries.get(i).icon(), sliceCenter(i, count), (INNER + OUTER) / 2.0, centerY);
        }
        if (focused != null) {
            for (int j = 0; j < focused.subs().size(); j++) {
                drawIcon(graphics, focused.subs().get(j).icon(), subStart + (j + 0.5) * SUB_SLICE_DEGREES, (SUB_INNER + SUB_OUTER) / 2.0, centerY);
            }

            Component label = focused.name();
            if (hoveredSub >= 0 && hoveredSub < focused.subs().size()) {
                label = Component.empty().append(focused.name()).append(" › ").append(focused.subs().get(hoveredSub).name());
            }
            graphics.text(client.font, label, HUD_MARGIN, centerY - SUB_OUTER - 12, 0xFFFFFFFF, true);
        }
    }

    private static int cellColor(int color, double x, double y, int count, @Nullable Entry focused, double subStart, boolean[] currentMain, boolean[] currentSub) {
        double radius = Math.hypot(x, y);
        double angle = Math.toDegrees(Math.atan2(y, x));

        if (radius >= INNER && radius < OUTER) {
            double width = sliceWidth(count);
            double offset = (angle + 90.0) % width;
            if (offset < GAP_DEGREES / 2.0 || offset > width - GAP_DEGREES / 2.0) return 0;
            int index = sliceAt(angle, count);
            return sliceColor(color, index == hoveredEntry && hoveredSub < 0, currentMain[index]);
        }
        if (focused != null && !focused.subs().isEmpty() && radius >= SUB_INNER && radius < SUB_OUTER) {
            double local = angle - subStart;
            if (local < 0.0 || local >= focused.subs().size() * SUB_SLICE_DEGREES) return 0;
            double offset = local % SUB_SLICE_DEGREES;
            if (offset < GAP_DEGREES / 2.0 || offset > SUB_SLICE_DEGREES - GAP_DEGREES / 2.0) return 0;
            int index = (int) (local / SUB_SLICE_DEGREES);
            return sliceColor(color, index == hoveredSub, currentSub[index]);
        }
        return 0;
    }

    private static void drawIcon(GuiGraphicsExtractor graphics, Item icon, double degrees, double radius, int centerY) {
        double angle = Math.toRadians(degrees);
        int x = (int) Math.round(Math.cos(angle) * radius) - 8;
        int y = centerY + (int) Math.round(Math.sin(angle) * radius) - 8;
        graphics.item(stack(icon), x, y);
    }

    /** The bottom-left readout of the selected construct, its sub-mode and its size. */
    private static void extractIndicator(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || PowerRingItem.getWornCorps(player).isEmpty()) return;
        Optional<ConstructRingPower> selected = ArmedRingPower.selectedConstruct(player);
        if (selected.isEmpty()) return;

        ConstructRingPower construct = selected.get();
        Entry entry = entries.stream().filter(candidate -> candidate.constructs().contains(construct)).findFirst().orElse(null);
        Optional<Sub> sub = entry == null ? Optional.empty() : entry.currentSub();

        Item icon = sub.map(Sub::icon).orElse(icon(construct));
        Component title = Component.translatable(construct.getTranslationKey());
        if (construct == RingPowerRegistry.SCULPT && sub.isPresent()) {
            title = Component.empty().append(title).append(" · ").append(sub.get().name());
        }
        Component detail = construct.usesSize()
                ? Component.translatable("hud.brightestday.construct_size", construct.describeSize(ConstructClient.size(construct)))
                : null;

        Font font = client.font;
        int color = ARGB.opaque(CorpsColors.of(player));
        int textWidth = Math.max(font.width(title), detail == null ? 0 : font.width(detail));
        int width = HUD_PADDING * 3 + 16 + textWidth;
        int height = HUD_PADDING * 2 + 16;
        int left = HUD_MARGIN;
        int top = graphics.guiHeight() - HUD_MARGIN - height;

        graphics.fill(left, top, left + width, top + height, ARGB.color(0x90, darken(color, 0.25F)));
        graphics.fill(left, top, left + 1, top + height, ARGB.color(0xFF, color));
        graphics.item(stack(icon), left + HUD_PADDING, top + HUD_PADDING);
        int textX = left + HUD_PADDING * 2 + 16;
        if (detail == null) {
            graphics.text(font, title, textX, top + HUD_PADDING + 4, 0xFFFFFFFF, true);
        } else {
            graphics.text(font, title, textX, top + HUD_PADDING - 1, 0xFFFFFFFF, true);
            graphics.text(font, detail, textX, top + HUD_PADDING + 9, 0xFFB8C4BA, true);
        }
    }

    private ConstructWheel() {}
}
