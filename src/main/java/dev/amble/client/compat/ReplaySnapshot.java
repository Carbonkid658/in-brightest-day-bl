package dev.amble.client.compat;

import dev.amble.client.effects.LightOrbEffects;
import dev.amble.client.effects.ShieldEffects;
import dev.amble.client.effects.WallEffects;
import dev.amble.client.effects.attacks.utility.LumberjackEffects;
import dev.amble.client.effects.attacks.utility.OreProbeEffects;
import dev.amble.client.effects.attacks.weapon.TurretEffects;
import dev.amble.client.effects.glide.GlideEffects;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.networking.payloads.s2c.PlayerStateS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

import java.util.function.Consumer;

public final class ReplaySnapshot {

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(PlayerStateS2CPayload.TYPE, (payload, context) -> {
            if (context.client().level == null || !(context.client().level.getEntity(payload.playerId()) instanceof Player player)) return;
            player.setAttached(BrightestDayAttachments.POWERS, payload.powers());
            player.setAttached(BrightestDayAttachments.RING, payload.ring());
            player.setAttached(BrightestDayAttachments.COLOR_TWEAK, payload.colorTweak());
            player.setAttached(BrightestDayAttachments.EYES, payload.eyes());
        });
    }

    public static void write(Consumer<CustomPacketPayload> out) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        for (AbstractClientPlayer player : client.level.players()) {
            out.accept(new PlayerStateS2CPayload(player.getId(), BrightestDayAttachments.get(player), BrightestDayAttachments.getRing(player),
                    BrightestDayAttachments.getColorTweak(player), BrightestDayAttachments.getEyes(player)));
        }
        WallEffects.snapshot(out);
        ShieldEffects.snapshot(out);
        LightOrbEffects.snapshot(out);
        TurretEffects.snapshot(out);
        GlideEffects.snapshot(out);
        LumberjackEffects.snapshot(out);
        OreProbeEffects.snapshot(out);
    }

    private ReplaySnapshot() {}
}
