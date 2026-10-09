package dev.amble.core.forge;

import dev.amble.core.BrightestDayItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Obsidian dropped into the void is crushed into Void Shards, one shard per obsidian.
 * Four shards craft one Void Matter.
 */
public final class VoidMatter {
    private static final double SEARCH_RADIUS = 128.0;

    public static void tick(ItemEntity item) {
        if (!(item.level() instanceof ServerLevel level) || item.isRemoved()) return;
        if (item.getY() >= level.getMinY()) return;

        ItemStack stack = item.getItem();
        if (!stack.is(Items.OBSIDIAN)) return;
        int shards = stack.getCount();
        if (shards <= 0) return;

        double x = item.getX();
        double z = item.getZ();
        ItemStack result = new ItemStack(BrightestDayItems.VOID_SHARD, shards);
        item.discard();

        level.sendParticles(ParticleTypes.PORTAL, x, level.getMinY() + 1.0, z, 40, 0.4, 0.4, 0.4, 0.5);
        level.playSound(null, x, level.getMinY() + 1.0, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.2F, 0.5F);

        double[] at = destination(level, item, x, z);
        ItemEntity matterEntity = new ItemEntity(level, at[0], at[1], at[2], result);
        matterEntity.setDeltaMovement(0.0, 0.2, 0.0);
        level.addFreshEntity(matterEntity);
        level.sendParticles(ParticleTypes.PORTAL, at[0], at[1], at[2], 30, 0.3, 0.3, 0.3, 0.4);
    }

    // The void is no place to leave something: bring it back to whoever dropped it, else the surface, else the nearest player.
    private static double[] destination(ServerLevel level, ItemEntity item, double x, double z) {
        if (item.getOwner() instanceof ServerPlayer owner && owner.level() == level) {
            return new double[]{owner.getX(), owner.getY() + 0.5, owner.getZ()};
        }
        if (level.hasChunkAt(BlockPos.containing(x, 0, z))) {
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            if (surface > level.getMinY() + 1) return new double[]{x, surface + 0.5, z};
        }
        ServerPlayer nearest = null;
        double best = SEARCH_RADIUS * SEARCH_RADIUS;
        for (ServerPlayer player : level.players()) {
            double distance = player.distanceToSqr(x, player.getY(), z);
            if (distance < best) {
                best = distance;
                nearest = player;
            }
        }
        if (nearest != null) return new double[]{nearest.getX(), nearest.getY() + 0.5, nearest.getZ()};
        return new double[]{x, level.getMinY() + 1.0, z};
    }

    private VoidMatter() {}
}
