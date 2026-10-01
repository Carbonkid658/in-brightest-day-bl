package dev.amble.core.flight;

import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class FlightBoost {

    private static final Set<Player> BOOSTING = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    public static boolean isBoosting(Player player) {
        return BOOSTING.contains(player);
    }

    public static void setBoosting(Player player, boolean boosting) {
        if (boosting) {
            BOOSTING.add(player);
        } else {
            BOOSTING.remove(player);
        }
    }

    private FlightBoost() {}
}
