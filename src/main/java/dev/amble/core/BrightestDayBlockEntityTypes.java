package dev.amble.core;

import dev.amble.BrightestDay;
import dev.amble.core.blockentities.LanternBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BrightestDayBlockEntityTypes {
    public static final BlockEntityType<LanternBlockEntity> LANTERN_BLOCK_ENTITY_TYPE = register("lantern_block_entity",
            FabricBlockEntityTypeBuilder.create(LanternBlockEntity::new,
                    BrightestDayBlocks.GREEN_LANTERN_BLOCK,
                    BrightestDayBlocks.YELLOW_LANTERN_BLOCK)
                    .build());

    private static <T extends BlockEntityType<?>> T register(String name, T blockEntity) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, BrightestDay.id(name), blockEntity);
    }

    public static void init() {}
}
