package dev.amble.core.ringpowers;

import dev.amble.core.items.PowerRingItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.Set;

public final class CorpsMimicry {
    private static final double RANGE = 24.0;

    public static Set<LanternCorps> mimicked(ServerPlayer player, LanternCorps corps) {
        EnumSet<LanternCorps> nearby = EnumSet.noneOf(LanternCorps.class);
        if (corps != LanternCorps.INDIGO && corps != LanternCorps.BLACK) return nearby;

        for (Player other : player.level().players()) {
            if (other == player || other.isSpectator() || other.distanceToSqr(player) > RANGE * RANGE) continue;
            PowerRingItem.getWornCorps(other).ifPresent(nearby::add);
        }
        nearby.remove(corps);
        return nearby;
    }

    private CorpsMimicry() {}
}
