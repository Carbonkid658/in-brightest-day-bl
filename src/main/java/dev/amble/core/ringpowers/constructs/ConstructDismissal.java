package dev.amble.core.ringpowers.constructs;

import dev.amble.core.beams.BeamManager;
import dev.amble.core.beams.HealBeamManager;
import dev.amble.core.light.LightOrbManager;
import dev.amble.core.sculpt.SculptManager;
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
        long sculptCreated = SculptManager.latestCreatedAt(player.getUUID());
        long orbCreated = LightOrbManager.latestCreatedAt(player.getUUID());
        long toolCreated = ConstructTools.latestCreatedAt(player);
        long newest = Math.max(Math.max(Math.max(shieldCreated, sculptCreated), Math.max(wallCreated, toolCreated)), orbCreated);
        if (newest == Long.MIN_VALUE) {
            player.sendOverlayMessage(Component.translatable("message.brightestday.no_constructs_to_dismiss"));
            return;
        }

        if (newest == toolCreated) {
            ConstructTools.dissolveAll(player);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8F, 1.4F);
        } else if (newest == orbCreated) {
            LightOrbManager.dismissLatest(player.getUUID());
        } else if (newest == sculptCreated) {
            SculptManager.dismissLatest(player.getUUID());
        } else if (newest == wallCreated) {
            WallManager.dismissLatest(player.getUUID());
        } else {
            ShieldManager.dismissLatest(player.getUUID());
        }
    }

    public static void dismissAll(ServerPlayer player) {
        BeamManager.stop(player);
        HealBeamManager.stop(player);
        SculptManager.stop(player, false);
        ShieldManager.dismissAll(player.getUUID());
        WallManager.dismissAll(player.getUUID());
        SculptManager.dismissAll(player.getUUID());
        LightOrbManager.dismissAll(player.getUUID());
        ConstructTools.dissolveAll(player);
    }

    private ConstructDismissal() {}
}
