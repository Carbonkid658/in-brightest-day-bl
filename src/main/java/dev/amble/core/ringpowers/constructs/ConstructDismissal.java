package dev.amble.core.ringpowers.constructs;

import dev.amble.core.shields.ShieldManager;
import dev.amble.core.walls.WallManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class ConstructDismissal {

    public static void dismissLatest(ServerPlayer player) {
        long shieldCreated = ShieldManager.latestCreatedAt(player.getUUID());
        long wallCreated = WallManager.latestCreatedAt(player.getUUID());
        long toolCreated = ConstructTools.latestCreatedAt(player);
        long newest = Math.max(shieldCreated, Math.max(wallCreated, toolCreated));
        if (newest == Long.MIN_VALUE) {
            player.sendOverlayMessage(Component.translatable("message.brightestday.no_constructs_to_dismiss"));
            return;
        }

        if (newest == toolCreated) {
            ConstructTools.dissolveAll(player);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8F, 1.4F);
        } else if (newest == wallCreated) {
            WallManager.dismissLatest(player.getUUID());
        } else {
            ShieldManager.dismissLatest(player.getUUID());
        }
    }

    public static void dismissAll(ServerPlayer player) {
        ShieldManager.dismissAll(player.getUUID());
        WallManager.dismissAll(player.getUUID());
        ConstructTools.dissolveAll(player);
    }

    private ConstructDismissal() {}
}
