package dev.amble.core;

import dev.amble.BrightestDay;
import dev.amble.core.blocks.LanternBlock;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class BrightestDayCreativeTabs {
    public static final CreativeModeTab BRIGHTEST_DAY = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            BrightestDay.id("brightest_day"),
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.brightestday.brightest_day"))
                    .icon(() -> new ItemStack(BrightestDayItems.GREEN_POWER_RING))
                    .displayItems((parameters, output) -> {
                        BrightestDayItems.rings().values().forEach(output::accept);
                        BuiltInRegistries.BLOCK.stream()
                                .filter(block -> block instanceof LanternBlock)
                                .forEach(output::accept);
                        output.accept(BrightestDayBlocks.BLUE_LANTERN_SHRINE);
                        output.accept(BrightestDayBlocks.SPECTRUM_FORGE);
                        output.accept(BrightestDayBlocks.ZAMARONIAN_CRYSTAL);
                        output.accept(BrightestDayBlocks.YELLOW_BATTERY_CORE);
                        output.accept(BrightestDayBlocks.SAPPHIRE_BATTERY_CORE);
                    })
                    .build()
    );

    public static void init() {}
}
