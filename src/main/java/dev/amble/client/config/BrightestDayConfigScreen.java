package dev.amble.client.config;

import dev.amble.config.BrightestDayConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class BrightestDayConfigScreen {
    private static final String PREFIX = "config.brightestday.";

    private BrightestDayConfigScreen() {}

    public static Screen create(@Nullable Screen parent) {
        return YetAnotherConfigLib.create(BrightestDayConfig.HANDLER, (defaults, config, builder) -> builder
                .title(Component.translatable(PREFIX + "title"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable(PREFIX + "category.constructs"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable(PREFIX + "group.blast"))
                                .option(doubleSlider("blast_range", defaults.blastRange, () -> config.blastRange, v -> config.blastRange = v, 8.0, 128.0, 1.0))
                                .option(doubleSlider("blast_radius", defaults.blastRadius, () -> config.blastRadius, v -> config.blastRadius = v, 1.0, 16.0, 0.5))
                                .option(floatSlider("blast_direct_damage", defaults.blastDirectDamage, () -> config.blastDirectDamage, v -> config.blastDirectDamage = v, 0.0F, 40.0F, 0.5F))
                                .option(floatSlider("blast_splash_damage", defaults.blastSplashDamage, () -> config.blastSplashDamage, v -> config.blastSplashDamage = v, 0.0F, 40.0F, 0.5F))
                                .option(doubleSlider("blast_knockback", defaults.blastKnockback, () -> config.blastKnockback, v -> config.blastKnockback = v, 0.0, 5.0, 0.1))
                                .option(floatSlider("blast_explosion_power", defaults.blastExplosionPower, () -> config.blastExplosionPower, v -> config.blastExplosionPower = v, 0.0F, 8.0F, 0.1F))
                                .option(toggle("blast_breaks_blocks", defaults.blastBreaksBlocks, () -> config.blastBreaksBlocks, v -> config.blastBreaksBlocks = v))
                                .option(doubleSlider("blast_assist_cone", defaults.blastAssistConeDegrees, () -> config.blastAssistConeDegrees, v -> config.blastAssistConeDegrees = v, 0.0, 45.0, 1.0))
                                .option(doubleSlider("blast_homing_cone", defaults.blastHomingConeDegrees, () -> config.blastHomingConeDegrees, v -> config.blastHomingConeDegrees = v, 0.0, 90.0, 1.0))
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable(PREFIX + "group.beam"))
                                .option(floatSlider("beam_damage", defaults.beamDamage, () -> config.beamDamage, v -> config.beamDamage = v, 0.0F, 20.0F, 0.5F))
                                .option(intSlider("beam_max_ticks", defaults.beamMaxTicks, () -> config.beamMaxTicks, v -> config.beamMaxTicks = v, 20, 600, 10))
                                .option(intSlider("beam_drain", defaults.beamDrainPerSecond, () -> config.beamDrainPerSecond, v -> config.beamDrainPerSecond = v, 0, 200, 1))
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable(PREFIX + "group.heal_beam"))
                                .option(floatSlider("heal_beam_amount", defaults.healBeamAmount, () -> config.healBeamAmount, v -> config.healBeamAmount = v, 0.0F, 10.0F, 0.5F))
                                .option(intSlider("heal_beam_max_ticks", defaults.healBeamMaxTicks, () -> config.healBeamMaxTicks, v -> config.healBeamMaxTicks = v, 20, 1200, 10))
                                .option(intSlider("heal_beam_drain", defaults.healBeamDrainPerSecond, () -> config.healBeamDrainPerSecond, v -> config.healBeamDrainPerSecond = v, 0, 200, 1))
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable(PREFIX + "group.wall"))
                                .option(intSlider("wall_lifetime", defaults.wallLifetimeTicks, () -> config.wallLifetimeTicks, v -> config.wallLifetimeTicks = v, 20, 6000, 20))
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable(PREFIX + "category.sculpt"))
                        .option(intSlider("sculpt_block_cost", defaults.sculptBlockCost, () -> config.sculptBlockCost, v -> config.sculptBlockCost = v, 0, 20, 1))
                        .option(intField("sculpt_max_blocks", defaults.sculptMaxBlocks, () -> config.sculptMaxBlocks, v -> config.sculptMaxBlocks = v, 1, 65536))
                        .option(intField("sculpt_max_total_blocks", defaults.sculptMaxTotalBlocks, () -> config.sculptMaxTotalBlocks, v -> config.sculptMaxTotalBlocks = v, 1, 262144))
                        .option(intField("sculpt_blocks_per_upkeep", defaults.sculptBlocksPerUpkeep, () -> config.sculptBlocksPerUpkeep, v -> config.sculptBlocksPerUpkeep = v, 1, 65536))
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable(PREFIX + "category.ring"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable(PREFIX + "group.charge"))
                                .description(OptionDescription.of(Component.translatable(PREFIX + "group.charge.desc")))
                                .option(intSlider("charge_blast", defaults.blastChargeTicks, () -> config.blastChargeTicks, v -> config.blastChargeTicks = v, 0, 100, 1))
                                .option(intSlider("charge_beam", defaults.beamChargeTicks, () -> config.beamChargeTicks, v -> config.beamChargeTicks = v, 0, 100, 1))
                                .option(intSlider("charge_heal_beam", defaults.healBeamChargeTicks, () -> config.healBeamChargeTicks, v -> config.healBeamChargeTicks = v, 0, 100, 1))
                                .option(intSlider("charge_entity_shield", defaults.entityShieldChargeTicks, () -> config.entityShieldChargeTicks, v -> config.entityShieldChargeTicks = v, 0, 100, 1))
                                .option(intSlider("charge_area_shield", defaults.areaShieldChargeTicks, () -> config.areaShieldChargeTicks, v -> config.areaShieldChargeTicks = v, 0, 100, 1))
                                .option(intSlider("charge_wall", defaults.wallChargeTicks, () -> config.wallChargeTicks, v -> config.wallChargeTicks = v, 0, 100, 1))
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable(PREFIX + "group.concussive"))
                                .option(doubleSlider("concussive_range", defaults.concussiveRange, () -> config.concussiveRange, v -> config.concussiveRange = v, 1.0, 24.0, 0.5))
                                .option(doubleSlider("concussive_cone", defaults.concussiveConeDegrees, () -> config.concussiveConeDegrees, v -> config.concussiveConeDegrees = v, 5.0, 180.0, 5.0))
                                .option(doubleSlider("concussive_knockback", defaults.concussiveKnockback, () -> config.concussiveKnockback, v -> config.concussiveKnockback = v, 0.0, 6.0, 0.1))
                                .option(floatSlider("concussive_damage", defaults.concussiveDamage, () -> config.concussiveDamage, v -> config.concussiveDamage = v, 0.0F, 20.0F, 0.5F))
                                .option(intSlider("concussive_cost", defaults.concussiveCost, () -> config.concussiveCost, v -> config.concussiveCost = v, 0, 500, 5))
                                .option(intSlider("concussive_cooldown", defaults.concussiveCooldownTicks, () -> config.concussiveCooldownTicks, v -> config.concussiveCooldownTicks = v, 0, 200, 1))
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable(PREFIX + "group.acid"))
                                .option(doubleSlider("acid_range", defaults.acidRange, () -> config.acidRange, v -> config.acidRange = v, 1.0, 16.0, 0.5))
                                .option(floatSlider("acid_damage", defaults.acidDamage, () -> config.acidDamage, v -> config.acidDamage = v, 0.0F, 20.0F, 0.5F))
                                .option(intSlider("acid_fire_seconds", defaults.acidFireSeconds, () -> config.acidFireSeconds, v -> config.acidFireSeconds = v, 0, 20, 1))
                                .option(intSlider("acid_armor_wear", defaults.acidArmorWear, () -> config.acidArmorWear, v -> config.acidArmorWear = v, 0, 20, 1))
                                .option(intSlider("acid_drain", defaults.acidDrainPerSecond, () -> config.acidDrainPerSecond, v -> config.acidDrainPerSecond = v, 0, 200, 1))
                                .build())
                        .build())
        ).generateScreen(parent);
    }

    private static <T> Option.Builder<T> option(String key, T def, Supplier<T> getter, Consumer<T> setter) {
        return Option.<T>createBuilder()
                .name(Component.translatable(PREFIX + "option." + key))
                .description(OptionDescription.of(Component.translatable(PREFIX + "option." + key + ".desc")))
                .binding(def, getter, setter);
    }

    private static Option<Boolean> toggle(String key, boolean def, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return option(key, def, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    private static Option<Integer> intSlider(String key, int def, Supplier<Integer> getter, Consumer<Integer> setter, int min, int max, int step) {
        return option(key, def, getter, setter)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(min, max).step(step))
                .build();
    }

    private static Option<Integer> intField(String key, int def, Supplier<Integer> getter, Consumer<Integer> setter, int min, int max) {
        return option(key, def, getter, setter)
                .controller(opt -> IntegerFieldControllerBuilder.create(opt).range(min, max))
                .build();
    }

    private static Option<Float> floatSlider(String key, float def, Supplier<Float> getter, Consumer<Float> setter, float min, float max, float step) {
        return option(key, def, getter, setter)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(min, max).step(step))
                .build();
    }

    private static Option<Double> doubleSlider(String key, double def, Supplier<Double> getter, Consumer<Double> setter, double min, double max, double step) {
        return option(key, def, getter, setter)
                .controller(opt -> DoubleSliderControllerBuilder.create(opt).range(min, max).step(step))
                .build();
    }
}
