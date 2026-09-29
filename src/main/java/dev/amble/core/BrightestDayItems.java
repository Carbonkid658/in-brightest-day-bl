package dev.amble.core;

import dev.amble.BrightestDay;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.LanternCorps;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public class BrightestDayItems {
    private static final Map<LanternCorps, Item> RINGS = new EnumMap<>(LanternCorps.class);

    public static final Item GREEN_POWER_RING = registerRing("green_power_ring", LanternCorps.GREEN);
    public static final Item YELLOW_POWER_RING = registerRing("yellow_power_ring", LanternCorps.YELLOW);
    public static final Item RED_POWER_RING = registerRing("red_power_ring", LanternCorps.RED);
    public static final Item ORANGE_POWER_RING = registerRing("orange_power_ring", LanternCorps.ORANGE);
    public static final Item BLUE_POWER_RING = registerRing("blue_power_ring", LanternCorps.BLUE);
    public static final Item INDIGO_POWER_RING = registerRing("indigo_power_ring", LanternCorps.INDIGO);
    public static final Item STAR_SAPPHIRE_POWER_RING = registerRing("star_sapphire_power_ring", LanternCorps.STAR_SAPPHIRE);
    public static final Item WHITE_POWER_RING = registerRing("white_power_ring", LanternCorps.WHITE);
    public static final Item BLACK_POWER_RING = registerRing("black_power_ring", LanternCorps.BLACK);

    public static Item ring(LanternCorps corps) {
        return RINGS.get(corps);
    }

    public static Map<LanternCorps, Item> rings() {
        return Collections.unmodifiableMap(RINGS);
    }

    private static Item registerRing(String name, LanternCorps corps) {
        Item ring = register(name, id -> new PowerRingItem(
                new Item.Properties()
                        .setId(ResourceKey.create(Registries.ITEM, id))
                        .fireResistant()
                        .stacksTo(1)
                        .component(BrightestDayComponents.POWER_TYPE, 0)
                        .component(BrightestDayComponents.LANTERN_CORPS, corps)
                        .component(DataComponents.TOOLTIP_STYLE, BrightestDay.id("ring"))
        ));
        RINGS.put(corps, ring);
        return ring;
    }

    public static Item register(String name, Function<Identifier, Item> factory) {
        Identifier id = BrightestDay.id(name);
        Item item = factory.apply(id);

        return Registry.register(BuiltInRegistries.ITEM, id, item);
    }

    public static void init() {}
}
