package dev.amble.core;

import dev.amble.BrightestDay;
import dev.amble.core.menus.LanternMenu;
import dev.amble.core.menus.MannequinMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class BrightestDayMenus {
    public static final MenuType<LanternMenu> LANTERN = Registry.register(
            BuiltInRegistries.MENU,
            BrightestDay.id("lantern"),
            new MenuType<>(LanternMenu::new, FeatureFlags.VANILLA_SET)
    );

    public static final MenuType<MannequinMenu> MANNEQUIN = Registry.register(
            BuiltInRegistries.MENU,
            BrightestDay.id("mannequin"),
            new ExtendedMenuType<>(MannequinMenu::new, ByteBufCodecs.VAR_INT)
    );

    public static void init() {}
}
