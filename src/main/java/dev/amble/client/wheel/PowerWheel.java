package dev.amble.client.wheel;

import dev.amble.BrightestDay;
import dev.amble.client.BrightestDayKeybinds;
import dev.amble.client.effects.ConstructClient;
import dev.amble.client.effects.SculptClient;
import dev.amble.core.drill.DrillMode;
import dev.amble.core.drill.DrillModes;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.networking.payloads.c2s.DrillModeC2SPayload;
import dev.amble.core.networking.payloads.c2s.SelectAbilityC2SPayload;
import dev.amble.core.networking.payloads.c2s.SelectConstructC2SPayload;
import dev.amble.core.ringpowers.CorpsColors;
import dev.amble.core.ringpowers.RingPower;
import dev.amble.core.ringpowers.RingPowerRegistry;
import dev.amble.core.ringpowers.constructs.ConstructRingPower;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import dev.amble.core.sculpt.SculptShape;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

public final class PowerWheel {
    private static final int INNER = 30;
    private static final int OUTER = 92;
    private static final int SUB_INNER = 98;
    private static final int SUB_OUTER = 146;
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

    private static final Map<RingPower<?>, Item> ICONS = Map.ofEntries(
            Map.entry(RingPowerRegistry.BLAST, Items.FIRE_CHARGE),
            Map.entry(RingPowerRegistry.BEAM, Items.END_ROD),
            Map.entry(RingPowerRegistry.HEAL_BEAM, Items.GLISTERING_MELON_SLICE),
            Map.entry(RingPowerRegistry.ENTITY_SHIELD, Items.HEART_OF_THE_SEA),
            Map.entry(RingPowerRegistry.AREA_SHIELD, Items.TURTLE_HELMET),
            Map.entry(RingPowerRegistry.WALL, Items.STONE_BRICKS),
            Map.entry(RingPowerRegistry.SCULPT, Items.AMETHYST_SHARD),
            Map.entry(RingPowerRegistry.LIGHT_ORB, Items.GLOWSTONE),
            Map.entry(RingPowerRegistry.DRILL, Items.DIAMOND_PICKAXE),
            Map.entry(RingPowerRegistry.GLIDER, Items.ELYTRA),
            Map.entry(RingPowerRegistry.GRAPPLING_HOOK, Items.TRIPWIRE_HOOK),
            Map.entry(RingPowerRegistry.LUMBERJACK, Items.IRON_AXE),
            Map.entry(RingPowerRegistry.ORE_PROBE, Items.SPYGLASS),
            Map.entry(RingPowerRegistry.SWARM_MISSILES, Items.FIREWORK_ROCKET),
            Map.entry(RingPowerRegistry.PIERCING_LANCE, Items.SPECTRAL_ARROW),
            Map.entry(RingPowerRegistry.CHAIN_BOLT, Items.TRIDENT),
            Map.entry(RingPowerRegistry.BOOMERANG_DISC, Items.MUSIC_DISC_PIGSTEP),
            Map.entry(RingPowerRegistry.NOVA_BURST, Items.FIREWORK_STAR),
            Map.entry(RingPowerRegistry.GROUND_SLAM, Items.MACE),
            Map.entry(RingPowerRegistry.RAPID_BARRAGE, Items.PRISMARINE_SHARD),
            Map.entry(RingPowerRegistry.GIANT_FIST, Items.HEAVY_CORE),
            Map.entry(RingPowerRegistry.ENERGY_WHIP, Items.BREEZE_ROD),
            Map.entry(RingPowerRegistry.SENTRY_TURRET, Items.DISPENSER),
            Map.entry(RingPowerRegistry.TOOL_FORGE, Items.ANVIL),
            Map.entry(RingPowerRegistry.TRACTOR_BEAM, Items.LEAD),
            Map.entry(RingPowerRegistry.SCAN, Items.SPYGLASS),
            Map.entry(RingPowerRegistry.CONCUSSIVE, Items.WIND_CHARGE),
            Map.entry(RingPowerRegistry.ACID, Items.MAGMA_CREAM)
    );
    private static final Map<SculptShape, Item> SHAPE_ICONS = Map.of(
            SculptShape.FREEFORM, Items.FEATHER,
            SculptShape.STAIRS, Items.OAK_STAIRS,
            SculptShape.TUBE, Items.HOPPER,
            SculptShape.CAGE, Items.IRON_BARS
    );
    private static final List<Group> GROUPS = List.of(
            new Group("construct_group.brightestday.attacks", Items.BLAZE_ROD, List.of("blast", "swarm_missiles", "piercing_lance", "chain_bolt", "boomerang_disc", "rapid_barrage", "nova_burst", "ground_slam")),
            new Group("construct_group.brightestday.weapons", Items.IRON_SWORD, List.of("giant_fist", "energy_whip", "sentry_turret")),
            new Group("construct_group.brightestday.utility", Items.COMPASS, List.of("glider", "grappling_hook", "light_orb", "lumberjack", "ore_probe")),
            new Group("construct_group.brightestday.shield", Items.SHIELD, List.of("entity_shield", "area_shield"))
    );
    private static final Map<DrillMode, Item> DRILL_MODE_ICONS = Map.of(
            DrillMode.HOLD, Items.DIAMOND_PICKAXE,
            DrillMode.TUNNEL, Items.DIAMOND_SHOVEL
    );
    private static final Item FALLBACK_ICON = Items.NETHER_STAR;
    private static final Map<Item, ItemStack> STACKS = new HashMap<>();

    private static final PowerWheel CONSTRUCTS = new PowerWheel("construct_wheel", BrightestDayKeybinds.CYCLE_CONSTRUCT, false,
            PowerWheel::constructEntries, player -> ArmedRingPower.selectedConstruct(player).map(construct -> construct), PowerWheel::tapConstructs);
    private static final PowerWheel ABILITIES = new PowerWheel("ability_wheel", BrightestDayKeybinds.ABILITY_WHEEL, true,
            PowerWheel::abilityEntries, ArmedRingPower::selectedAbility, PowerWheel::tapAbilities);
    private static final List<PowerWheel> WHEELS = List.of(CONSTRUCTS, ABILITIES);

    private record Sub(Component name, Item icon, Runnable apply, BooleanSupplier current) {}

    private record Entry(Component name, Item icon, Set<RingPower<?>> powers, Runnable apply, List<Sub> subs) {
        Optional<Sub> currentSub() {
            return this.subs.stream().filter(sub -> sub.current().getAsBoolean()).findFirst();
        }
    }

    private final String id;
    private final KeyMapping key;
    private final boolean mirrored;
    private final Function<Player, List<Entry>> builder;
    private final Function<Player, Optional<RingPower<?>>> selection;
    private final Consumer<LocalPlayer> onTap;

    private List<Entry> entries = List.of();
    private boolean open;
    private int heldTicks;
    private double cursorX;
    private double cursorY;
    private double travelled;
    private int hoveredEntry = -1;
    private int hoveredSub = -1;

    private PowerWheel(String id, KeyMapping key, boolean mirrored, Function<Player, List<Entry>> builder,
                       Function<Player, Optional<RingPower<?>>> selection, Consumer<LocalPlayer> onTap) {
        this.id = id;
        this.key = key;
        this.mirrored = mirrored;
        this.builder = builder;
        this.selection = selection;
        this.onTap = onTap;
    }

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> DrillModes.setClient(DrillMode.HOLD));
        ClientTickEvents.END_CLIENT_TICK.register(client -> WHEELS.forEach(wheel -> wheel.tick(client)));
        for (PowerWheel wheel : WHEELS) {
            HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, BrightestDay.id(wheel.id), wheel::extractWheel);
        }
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, BrightestDay.id("power_indicator"), PowerWheel::extractIndicator);
    }

    public static boolean isOpen() {
        return WHEELS.stream().anyMatch(wheel -> wheel.open);
    }

    public static boolean onMouse(double dx, double dy) {
        for (PowerWheel wheel : WHEELS) {
            if (wheel.open) {
                wheel.steer(dx, dy);
                return true;
            }
        }
        return false;
    }

    private void steer(double dx, double dy) {
        double scale = MOUSE_SCALE / Minecraft.getInstance().getWindow().getGuiScale();
        double localX = (this.mirrored ? -dx : dx) * scale;
        this.cursorX = Math.max(this.cursorX + localX, 1.0);
        this.cursorY += dy * scale;
        this.travelled += Math.hypot(dx * scale, dy * scale);
        double length = Math.hypot(this.cursorX, this.cursorY);
        if (length > MAX_CURSOR) {
            this.cursorX *= MAX_CURSOR / length;
            this.cursorY *= MAX_CURSOR / length;
        }
        this.updateHover();
    }

    private boolean isCurrent(Player player, Entry entry) {
        return this.selection.apply(player).map(entry.powers()::contains).orElse(false);
    }

    private void tick(Minecraft client) {
        LocalPlayer player = client.player;
        this.entries = player == null ? List.of() : this.builder.apply(player);

        boolean pressed = false;
        while (this.key.consumeClick()) pressed = true;
        boolean down = player != null && client.gui.screen() == null && this.key.isDown();

        if (!this.open) {
            if (!pressed && !down) return;
            if (player == null) return;
            if (this.entries.isEmpty() || !down) {
                this.onTap.accept(player);
                return;
            }
            if (WHEELS.stream().anyMatch(wheel -> wheel != this && wheel.open)) return;
            this.openWheel(player);
            return;
        }

        if (down && !this.entries.isEmpty()) {
            this.heldTicks++;
            return;
        }
        this.open = false;
        if (this.heldTicks <= TAP_TICKS && this.travelled < TAP_DISTANCE) {
            if (player != null) this.onTap.accept(player);
        } else if (this.hoveredEntry >= 0 && this.hoveredEntry < this.entries.size()) {
            Entry entry = this.entries.get(this.hoveredEntry);
            if (this.hoveredSub >= 0 && this.hoveredSub < entry.subs().size()) {
                entry.subs().get(this.hoveredSub).apply().run();
            } else {
                entry.apply().run();
            }
        }
    }

    private void openWheel(LocalPlayer player) {
        this.open = true;
        this.heldTicks = 0;
        this.travelled = 0.0;
        this.hoveredSub = -1;
        this.hoveredEntry = 0;
        for (int i = 0; i < this.entries.size(); i++) {
            if (this.isCurrent(player, this.entries.get(i))) this.hoveredEntry = i;
        }
        double angle = Math.toRadians(sliceCenter(this.hoveredEntry, this.entries.size()));
        double radius = (INNER + OUTER) / 2.0;
        this.cursorX = Math.cos(angle) * radius;
        this.cursorY = Math.sin(angle) * radius;
    }

    private void updateHover() {
        if (this.entries.isEmpty()) return;
        double radius = Math.hypot(this.cursorX, this.cursorY);
        double angle = Math.toDegrees(Math.atan2(this.cursorY, this.cursorX));

        boolean latched = this.hoveredEntry >= 0 && this.hoveredEntry < this.entries.size()
                && !this.entries.get(this.hoveredEntry).subs().isEmpty() && radius >= SUB_LATCH;
        if (latched) {
            Entry entry = this.entries.get(this.hoveredEntry);
            double start = this.subStart(this.hoveredEntry, entry.subs().size());
            this.hoveredSub = Mth.clamp((int) Math.floor((angle - start) / subSlice(entry.subs().size())), 0, entry.subs().size() - 1);
        } else {
            this.hoveredEntry = sliceAt(angle, this.entries.size());
            this.hoveredSub = -1;
        }
    }

    private record Group(String key, Item icon, List<String> members) {
        int indexOf(ConstructRingPower construct) {
            return this.members.indexOf(construct.id().getPath());
        }
    }

    private static List<Entry> constructEntries(Player player) {
        List<ConstructRingPower> owned = ArmedRingPower.constructs(player);
        List<Entry> built = new ArrayList<>();
        Map<Group, Integer> slots = new HashMap<>();
        Map<Group, List<ConstructRingPower>> members = new HashMap<>();

        for (ConstructRingPower construct : owned) {
            Group group = GROUPS.stream().filter(candidate -> candidate.indexOf(construct) >= 0).findFirst().orElse(null);
            if (group != null) {
                if (!slots.containsKey(group)) {
                    slots.put(group, built.size());
                    built.add(null);
                }
                members.computeIfAbsent(group, key -> new ArrayList<>()).add(construct);
            } else if (construct == RingPowerRegistry.DRILL) {
                List<Sub> modes = new ArrayList<>();
                for (DrillMode mode : DrillMode.values()) {
                    modes.add(new Sub(Component.translatable(mode.translationKey()), DRILL_MODE_ICONS.get(mode), () -> {
                        setDrillMode(mode);
                        selectConstruct(player, construct);
                    }, () -> DrillModes.client() == mode));
                }
                built.add(new Entry(Component.translatable(construct.getTranslationKey()), icon(construct), Set.of(construct),
                        () -> selectConstruct(player, construct), modes));
            } else if (construct == RingPowerRegistry.SCULPT) {
                List<Sub> shapes = new ArrayList<>();
                for (SculptShape shape : SculptShape.values()) {
                    shapes.add(new Sub(Component.translatable(shape.translationKey()), SHAPE_ICONS.get(shape), () -> {
                        SculptClient.setShape(shape);
                        selectConstruct(player, construct);
                    }, () -> SculptClient.shape() == shape));
                }
                built.add(new Entry(Component.translatable(construct.getTranslationKey()), icon(construct), Set.of(construct),
                        () -> selectConstruct(player, construct), shapes));
            } else {
                built.add(new Entry(Component.translatable(construct.getTranslationKey()), icon(construct), Set.of(construct),
                        () -> selectConstruct(player, construct), List.of()));
            }
        }

        slots.forEach((group, slot) -> built.set(slot, groupEntry(player, group, members.get(group))));
        return built;
    }

    private static Entry groupEntry(Player player, Group group, List<ConstructRingPower> constructs) {
        List<ConstructRingPower> ordered = new ArrayList<>(constructs);
        ordered.sort(Comparator.comparingInt(group::indexOf));
        List<Sub> subs = new ArrayList<>();
        for (ConstructRingPower construct : ordered) {
            subs.add(new Sub(Component.translatable(construct.getTranslationKey()), icon(construct),
                    () -> selectConstruct(player, construct), () -> ArmedRingPower.selectedConstruct(player).orElse(null) == construct));
        }
        Set<RingPower<?>> powers = Set.copyOf(ordered);
        Sub first = subs.getFirst();
        return new Entry(Component.translatable(group.key()), group.icon(), powers, () -> {
            Optional<ConstructRingPower> current = ArmedRingPower.selectedConstruct(player);
            if (current.isPresent() && powers.contains(current.get())) {
                selectConstruct(player, current.get());
            } else {
                first.apply().run();
            }
        }, List.copyOf(subs));
    }

    private static List<Entry> abilityEntries(Player player) {
        List<Entry> built = new ArrayList<>();
        for (RingPower<?> ability : ArmedRingPower.abilities(player)) {
            built.add(new Entry(Component.translatable(ability.getTranslationKey()), icon(ability), Set.of(ability),
                    () -> selectAbility(player, ability), List.of()));
        }
        return built;
    }

    private static void setDrillMode(DrillMode mode) {
        if (DrillModes.client() == mode) return;
        DrillModes.setClient(mode);
        ClientPlayNetworking.send(new DrillModeC2SPayload(mode.ordinal()));
    }

    private static void selectConstruct(Player player, ConstructRingPower construct) {
        boolean selected = !ArmedRingPower.isAbilityMode(player) && ArmedRingPower.selectedConstruct(player).orElse(null) == construct;
        if (!selected) ClientPlayNetworking.send(new SelectConstructC2SPayload(construct.id()));
    }

    private static void selectAbility(Player player, RingPower<?> ability) {
        boolean selected = ArmedRingPower.activeAbility(player).orElse(null) == ability;
        if (!selected) ClientPlayNetworking.send(new SelectAbilityC2SPayload(ability.id()));
    }

    private static void tapConstructs(LocalPlayer player) {
        Optional<ConstructRingPower> current = ArmedRingPower.selectedConstruct(player);
        if (ArmedRingPower.isAbilityMode(player) && current.isPresent()) {
            selectConstruct(player, current.get());
        } else {
            ConstructClient.cycle();
        }
    }

    private static void tapAbilities(LocalPlayer player) {
        List<RingPower<?>> abilities = ArmedRingPower.abilities(player);
        Optional<RingPower<?>> current = ArmedRingPower.selectedAbility(player);
        if (abilities.isEmpty() || current.isEmpty()) return;
        if (!ArmedRingPower.isAbilityMode(player)) {
            selectAbility(player, current.get());
            return;
        }
        selectAbility(player, abilities.get((abilities.indexOf(current.get()) + 1) % abilities.size()));
    }

    private static ItemStack stack(Item item) {
        return STACKS.computeIfAbsent(item, ItemStack::new);
    }

    private static Item icon(RingPower<?> power) {
        return ICONS.getOrDefault(power, FALLBACK_ICON);
    }

    private static double subSlice(int count) {
        return Math.min(SUB_SLICE_DEGREES, 180.0 / Math.max(count, 1));
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

    private double subStart(int index, int subs) {
        double span = subs * subSlice(subs);
        return Mth.clamp(sliceCenter(index, this.entries.size()) - span / 2.0, -90.0, Math.max(90.0 - span, -90.0));
    }

    private static int darken(int color, float factor) {
        return ARGB.color(255, Math.round(ARGB.red(color) * factor), Math.round(ARGB.green(color) * factor), Math.round(ARGB.blue(color) * factor));
    }

    private static int sliceColor(int color, boolean hovered, boolean current) {
        if (hovered) return ARGB.color(0xB0, color);
        if (current) return ARGB.color(0x90, darken(color, 0.6F));
        return ARGB.color(0x70, darken(color, 0.3F));
    }

    private int screenX(GuiGraphicsExtractor graphics, int x) {
        return this.mirrored ? graphics.guiWidth() - x : x;
    }

    private void extractWheel(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (!this.open || player == null || this.entries.isEmpty()) return;

        int color = ARGB.opaque(CorpsColors.of(player));
        int centerY = graphics.guiHeight() / 2;
        int count = this.entries.size();
        Entry focused = this.hoveredEntry >= 0 && this.hoveredEntry < count ? this.entries.get(this.hoveredEntry) : null;
        double subStart = focused == null || focused.subs().isEmpty() ? 0.0 : this.subStart(this.hoveredEntry, focused.subs().size());
        boolean[] currentMain = new boolean[count];
        for (int i = 0; i < count; i++) currentMain[i] = this.isCurrent(player, this.entries.get(i));
        boolean[] currentSub = new boolean[focused == null ? 0 : focused.subs().size()];
        for (int j = 0; j < currentSub.length; j++) currentSub[j] = focused.subs().get(j).current().getAsBoolean();

        for (int y = -SUB_OUTER; y < SUB_OUTER; y += CELL) {
            int runStart = 0;
            int runColor = 0;
            for (int x = 0; x <= SUB_OUTER; x += CELL) {
                int cellColor = x == SUB_OUTER ? 0 : this.cellColor(color, x + CELL * 0.5, y + CELL * 0.5, count, focused, subStart, currentMain, currentSub);
                if (cellColor != runColor) {
                    if (runColor != 0) {
                        int a = this.screenX(graphics, runStart);
                        int b = this.screenX(graphics, x);
                        graphics.fill(Math.min(a, b), centerY + y, Math.max(a, b), centerY + y + CELL, runColor);
                    }
                    runStart = x;
                    runColor = cellColor;
                }
            }
        }

        for (int i = 0; i < count; i++) {
            this.drawIcon(graphics, this.entries.get(i).icon(), sliceCenter(i, count), (INNER + OUTER) / 2.0, centerY);
        }
        if (focused == null) return;
        for (int j = 0; j < focused.subs().size(); j++) {
            this.drawIcon(graphics, focused.subs().get(j).icon(), subStart + (j + 0.5) * subSlice(focused.subs().size()), (SUB_INNER + SUB_OUTER) / 2.0, centerY);
        }

        Component label = focused.name();
        if (this.hoveredSub >= 0 && this.hoveredSub < focused.subs().size()) {
            label = Component.empty().append(focused.name()).append(" › ").append(focused.subs().get(this.hoveredSub).name());
        }
        int labelX = this.mirrored ? graphics.guiWidth() - HUD_MARGIN - client.font.width(label) : HUD_MARGIN;
        graphics.text(client.font, label, labelX, centerY - SUB_OUTER - 12, 0xFFFFFFFF, true);
    }

    private int cellColor(int color, double x, double y, int count, @Nullable Entry focused, double subStart, boolean[] currentMain, boolean[] currentSub) {
        double radius = Math.hypot(x, y);
        double angle = Math.toDegrees(Math.atan2(y, x));

        if (radius >= INNER && radius < OUTER) {
            double width = sliceWidth(count);
            double offset = (angle + 90.0) % width;
            if (offset < GAP_DEGREES / 2.0 || offset > width - GAP_DEGREES / 2.0) return 0;
            int index = sliceAt(angle, count);
            return sliceColor(color, index == this.hoveredEntry && this.hoveredSub < 0, currentMain[index]);
        }
        if (focused != null && !focused.subs().isEmpty() && radius >= SUB_INNER && radius < SUB_OUTER) {
            double local = angle - subStart;
            double slice = subSlice(focused.subs().size());
            if (local < 0.0 || local >= focused.subs().size() * slice) return 0;
            double offset = local % slice;
            if (offset < GAP_DEGREES / 2.0 || offset > slice - GAP_DEGREES / 2.0) return 0;
            int index = (int) (local / slice);
            return sliceColor(color, index == this.hoveredSub, currentSub[index]);
        }
        return 0;
    }

    private void drawIcon(GuiGraphicsExtractor graphics, Item icon, double degrees, double radius, int centerY) {
        double angle = Math.toRadians(degrees);
        int x = this.screenX(graphics, (int) Math.round(Math.cos(angle) * radius)) - 8;
        int y = centerY + (int) Math.round(Math.sin(angle) * radius) - 8;
        graphics.item(stack(icon), x, y);
    }

    private static void extractIndicator(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || PowerRingItem.getWornCorps(player).isEmpty()) return;

        Item icon;
        Component title;
        Component detail = null;
        Optional<RingPower<?>> ability = ArmedRingPower.activeAbility(player);
        if (ability.isPresent()) {
            icon = icon(ability.get());
            title = Component.translatable(ability.get().getTranslationKey());
        } else {
            Optional<ConstructRingPower> selected = ArmedRingPower.selectedConstruct(player);
            if (selected.isEmpty()) return;

            ConstructRingPower construct = selected.get();
            Entry entry = CONSTRUCTS.entries.stream().filter(candidate -> candidate.powers().contains(construct)).findFirst().orElse(null);
            Optional<Sub> sub = entry == null ? Optional.empty() : entry.currentSub();
            icon = sub.map(Sub::icon).orElse(icon(construct));
            title = Component.translatable(construct.getTranslationKey());
            if ((construct == RingPowerRegistry.SCULPT || construct == RingPowerRegistry.DRILL) && sub.isPresent()) {
                title = Component.empty().append(title).append(" · ").append(sub.get().name());
            }
            if (construct.usesSize()) {
                detail = Component.translatable("hud.brightestday.construct_size", construct.describeSize(ConstructClient.size(construct)));
            }
        }

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
}
