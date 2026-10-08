package dev.amble.core.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class BatteryFrameBlock extends Block {

    public enum Material implements StringRepresentable {
        GOLD("gold", () -> Blocks.GOLD_BLOCK),
        AMETHYST("amethyst", () -> Blocks.AMETHYST_BLOCK),
        EMERALD("emerald", () -> Blocks.EMERALD_BLOCK),
        REDSTONE("redstone", () -> Blocks.REDSTONE_BLOCK),
        LAPIS("lapis", () -> Blocks.LAPIS_BLOCK),
        OBSIDIAN("obsidian", () -> Blocks.OBSIDIAN);

        private final String name;
        private final Supplier<Block> block;

        Material(String name, Supplier<Block> block) {
            this.name = name;
            this.block = block;
        }

        public Block block() {
            return this.block.get();
        }

        public static @Nullable Material of(Block block) {
            for (Material material : values()) {
                if (material.block() == block) return material;
            }
            return null;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final EnumProperty<Material> MATERIAL = EnumProperty.create("material", Material.class);

    public BatteryFrameBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(MATERIAL, Material.GOLD));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MATERIAL);
    }

    public static Block original(BlockState state) {
        return state.getValue(MATERIAL).block();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(original(state)));
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(original(state));
    }
}
