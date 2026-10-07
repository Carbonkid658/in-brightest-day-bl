package dev.amble.core.forge;

import dev.amble.core.BrightestDayItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

public final class Catalysts {
    private static final float WARDEN_CHANCE = 0.5F;
    private static final float ANCIENT_CITY_CHANCE = 0.08F;

    public static void init() {
        LootTableEvents.MODIFY.register((key, table, source, registries) -> {
            if (!source.isBuiltin()) return;
            if (EntityTypes.WARDEN.getDefaultLootTable().map(key::equals).orElse(false)) {
                table.withPool(pool(BrightestDayItems.PARALLAX_SHARD, WARDEN_CHANCE));
            } else if (key.equals(BuiltInLootTables.ANCIENT_CITY)) {
                table.withPool(pool(BrightestDayItems.PARALLAX_SHARD, ANCIENT_CITY_CHANCE));
            }
        });
    }

    public static boolean is(ItemStack stack) {
        return stack.is(BrightestDayItems.PARALLAX_SHARD) || stack.is(BrightestDayItems.ZAMARON_CRYSTAL);
    }

    private static LootPool.Builder pool(Item item, float chance) {
        return LootPool.lootPool().add(LootItem.lootTableItem(item)).when(LootItemRandomChanceCondition.randomChance(chance));
    }

    private Catalysts() {}
}
