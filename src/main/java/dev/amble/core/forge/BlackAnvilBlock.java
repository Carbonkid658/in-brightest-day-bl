package dev.amble.core.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biomes;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Black Anvil: the forge where black power rings are made. It works like the Spectrum Forge (use it,
 * then strike it with the hammer each time the ring closes) but needs no lava and asks nothing of the
 * smith's emotions. Looks and feels like a vanilla anvil.
 */
public class BlackAnvilBlock extends SpectrumForgeBlock {
    private static final VoxelShape BASE = Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);
    // Same shapes as a vanilla anvil. Z: the face runs north-south; X: east-west.
    private static final VoxelShape SHAPE_Z = Shapes.or(BASE,
            Block.box(4.0, 4.0, 3.0, 12.0, 5.0, 13.0),
            Block.box(6.0, 5.0, 4.0, 10.0, 10.0, 12.0),
            Block.box(3.0, 10.0, 0.0, 13.0, 16.0, 16.0));
    private static final VoxelShape SHAPE_X = Shapes.or(BASE,
            Block.box(3.0, 4.0, 4.0, 13.0, 5.0, 12.0),
            Block.box(4.0, 5.0, 6.0, 12.0, 10.0, 10.0),
            Block.box(0.0, 10.0, 3.0, 16.0, 16.0, 13.0));

    public BlackAnvilBlock(Properties properties) {
        super(null, false, ForgeRecipes::black, properties);
    }

    /** Only awake in the Deep Dark, and only where no light reaches it. */
    @Override
    protected @Nullable Component ritualBlocked(Level level, BlockPos pos) {
        if (!level.getBiome(pos).is(Biomes.DEEP_DARK)) return Component.translatable("forge.brightestday.ritual.deep_dark");
        BlockPos above = pos.above();
        if (Math.max(level.getBrightness(LightLayer.BLOCK, above), level.getBrightness(LightLayer.SKY, above)) > 0) {
            return Component.translatable("forge.brightestday.ritual.no_light");
        }
        return null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
    }
}
