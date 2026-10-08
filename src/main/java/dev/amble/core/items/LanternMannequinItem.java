package dev.amble.core.items;

import dev.amble.core.mannequin.Mannequins;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LanternMannequinItem extends Item {
    public LanternMannequinItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) return InteractionResult.FAIL;
        BlockPos pos = context.getClickedPos().above();
        Vec3 at = Vec3.atBottomCenterOf(pos);
        AABB space = EntityTypes.MANNEQUIN.getDimensions().makeBoundingBox(at.add(0.0, Mannequins.PAD_HEIGHT, 0.0));
        if (!context.getLevel().noCollision(null, space) || !context.getLevel().getEntities(null, space).isEmpty()) return InteractionResult.FAIL;
        if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;

        if (Mannequins.place(level, at, player.getYRot() + 180.0F, player) == null) return InteractionResult.FAIL;
        context.getItemInHand().consume(1, player);
        return InteractionResult.SUCCESS_SERVER;
    }
}
