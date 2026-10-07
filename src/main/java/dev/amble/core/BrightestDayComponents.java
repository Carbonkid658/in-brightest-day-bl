package dev.amble.core;

import com.mojang.serialization.Codec;
import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.ringpowers.constructs.ConstructToolData;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public class BrightestDayComponents {
    public static final int MAX_POWER = 5000;

    public static final DataComponentType<Integer> POWER_TYPE =
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                    BrightestDay.id("power_type"),
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
                            .build()
            );

    public static final DataComponentType<LanternCorps> LANTERN_CORPS =
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                    BrightestDay.id("lantern_corps"),
                    DataComponentType.<LanternCorps>builder()
                            .persistent(LanternCorps.CODEC)
                            .networkSynchronized(LanternCorps.STREAM_CODEC)
                            .build()
            );

    public static final DataComponentType<GlobalPos> BOUND_LANTERN =
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                    BrightestDay.id("bound_lantern"),
                    DataComponentType.<GlobalPos>builder()
                            .persistent(GlobalPos.CODEC)
                            .networkSynchronized(GlobalPos.STREAM_CODEC)
                            .build()
            );

    public static final DataComponentType<Integer> RING_DEATHS =
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                    BrightestDay.id("ring_deaths"),
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build()
            );

    public static final DataComponentType<Boolean> DORMANT =
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                    BrightestDay.id("dormant"),
                    DataComponentType.<Boolean>builder()
                            .persistent(Codec.BOOL)
                            .networkSynchronized(ByteBufCodecs.BOOL)
                            .build()
            );

    public static final DataComponentType<ConstructToolData> CONSTRUCT_TOOL =
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                    BrightestDay.id("construct_tool"),
                    DataComponentType.<ConstructToolData>builder()
                            .persistent(ConstructToolData.CODEC)
                            .networkSynchronized(ConstructToolData.STREAM_CODEC)
                            .build()
            );
}
