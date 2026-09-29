package dev.amble.core;

import dev.amble.BrightestDay;
import dev.amble.core.blockentities.GreenLanternBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BrightestDayBlockEntityTypes {
    public static final BlockEntityType<GreenLanternBlockEntity> GREEN_LANTERN_BLOCK_ENTITY_TYPE = register("green_lantern_block_entity",
            FabricBlockEntityTypeBuilder.create(GreenLanternBlockEntity::new, BrightestDayBlocks.GREEN_LANTERN_BLOCK).build());

    private static <T extends BlockEntityType<?>> T register(String name, T blockEntity) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, BrightestDay.id(name), blockEntity);
    }

    public static void init() {}
}
