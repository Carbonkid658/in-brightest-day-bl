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

import java.util.function.Function;

public class BrightestDayItems {
    public static final Item POWER_RING = register("power_ring", id -> new PowerRingItem(
            new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, id))
                    .fireResistant()
                    .stacksTo(1)
                    .component(BrightestDayComponents.POWER_TYPE, BrightestDayComponents.MAX_POWER)
                    .component(BrightestDayComponents.LANTERN_CORPS, LanternCorps.GREEN)
                    .component(DataComponents.TOOLTIP_STYLE, BrightestDay.id("ring"))
    ));

    public static Item register(String name, Function<Identifier, Item> factory) {
        Identifier id = BrightestDay.id(name);
        Item item = factory.apply(id);

        return Registry.register(BuiltInRegistries.ITEM, id, item);
    }

    public static void init() {}
}
