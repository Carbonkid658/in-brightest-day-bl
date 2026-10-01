package dev.amble.core.ringpowers.constructs;

import dev.amble.core.attacks.area.BarrageManager;
import dev.amble.core.attacks.area.SlamManager;
import dev.amble.core.attacks.projectile.DiscManager;
import dev.amble.core.attacks.weapon.TurretManager;
import dev.amble.core.beams.BeamManager;
import dev.amble.core.drill.DrillManager;
import dev.amble.core.beams.HealBeamManager;
import dev.amble.core.light.LightOrbManager;
import dev.amble.core.sculpt.SculptManager;
import dev.amble.core.shields.ShieldManager;
import dev.amble.core.walls.WallManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;

public final class ConstructDismissal {

    public static void init() {
        ServerPlayerEvents.LEAVE.register(ConstructDismissal::dismissAll);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> List.copyOf(server.getPlayerList().getPlayers()).forEach(ConstructDismissal::dismissAll));
    }

    public static void dismissLatest(ServerPlayer player) {
        long shieldCreated = ShieldManager.latestCreatedAt(player.getUUID());
        long wallCreated = WallManager.latestCreatedAt(player.getUUID());
        long sculptCreated = SculptManager.latestCreatedAt(player.getUUID());
        long orbCreated = LightOrbManager.latestCreatedAt(player.getUUID());
        long turretCreated = TurretManager.latestCreatedAt(player.getUUID());
        long toolCreated = ConstructTools.latestCreatedAt(player);
        long newest = Math.max(Math.max(Math.max(Math.max(shieldCreated, sculptCreated), Math.max(wallCreated, toolCreated)), orbCreated), turretCreated);
        if (newest == Long.MIN_VALUE) {
            player.sendOverlayMessage(Component.translatable("message.brightestday.no_constructs_to_dismiss"));
            return;
        }

        if (newest == toolCreated) {
            ConstructTools.dissolveAll(player);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8F, 1.4F);
        } else if (newest == turretCreated) {
            TurretManager.dismissLatest(player.getUUID());
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
        BarrageManager.stop(player);
        DrillManager.stop(player);
        SlamManager.stop(player);
        DiscManager.dismiss(player);
        ShieldManager.dismissAll(player.getUUID());
        WallManager.dismissAll(player.getUUID());
        SculptManager.dismissAll(player.getUUID());
        LightOrbManager.dismissAll(player.getUUID());
        TurretManager.dismissAll(player.getUUID());
        ConstructTools.dissolveAll(player);
    }

    private ConstructDismissal() {}
}
