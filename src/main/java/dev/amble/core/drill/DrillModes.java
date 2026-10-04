package dev.amble.core.drill;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DrillModes {
    private static final Map<UUID, DrillMode> SERVER = new ConcurrentHashMap<>();
    private static volatile DrillMode client = DrillMode.HOLD;

    public static void init() {
        ServerPlayerEvents.LEAVE.register(player -> SERVER.remove(player.getUUID()));
    }

    public static DrillMode get(Player player) {
        return player.level().isClientSide() ? client : SERVER.getOrDefault(player.getUUID(), DrillMode.HOLD);
    }

    public static void set(Player player, DrillMode mode) {
        SERVER.put(player.getUUID(), mode);
    }

    public static DrillMode client() {
        return client;
    }

    public static void setClient(DrillMode mode) {
        client = mode;
    }

    private DrillModes() {}
}
