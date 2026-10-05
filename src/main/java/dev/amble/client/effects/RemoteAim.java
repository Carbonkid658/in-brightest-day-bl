package dev.amble.client.effects;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public final class RemoteAim {
    private static final double SMOOTHING = 0.4;

    private static final Map<Player, Vec3[]> AIMS = new WeakHashMap<>();

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(RemoteAim::tick);
    }

    private static void tick(Minecraft client) {
        if (client.level == null) {
            AIMS.clear();
            return;
        }
        if (client.isPaused()) return;

        for (AbstractClientPlayer player : client.level.players()) {
            if (player == client.player) continue;

            Vec3 raw = player.getViewVector(1.0F);
            Vec3[] aim = AIMS.get(player);
            if (aim == null) {
                AIMS.put(player, new Vec3[]{raw, raw});
                continue;
            }
            aim[0] = aim[1];
            aim[1] = aim[1].lerp(raw, SMOOTHING).normalize();
        }
        AIMS.keySet().removeIf(Player::isRemoved);
    }

    public static Vec3 look(Player player, float partialTicks) {
        Vec3[] aim = player == Minecraft.getInstance().player ? null : AIMS.get(player);
        if (aim == null) return player.getViewVector(partialTicks);
        return aim[0].lerp(aim[1], partialTicks).normalize();
    }

    private RemoteAim() {}
}
