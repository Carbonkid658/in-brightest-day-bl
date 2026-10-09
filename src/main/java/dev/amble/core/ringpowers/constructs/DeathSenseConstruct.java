package dev.amble.core.ringpowers.constructs;

import dev.amble.BrightestDay;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.CorpsArsenal;
import dev.amble.core.ringpowers.DeathSense;
import dev.amble.core.ringpowers.LanternCorps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathSenseConstruct extends ConstructRingPower {
    private static final int USE_COST = 40;
    private static final int CHARGE_TICKS = 10;
    private static final long COOLDOWN_TICKS = 15L * 20L;

    private static final Map<UUID, Long> READY_AT = new HashMap<>();

    public DeathSenseConstruct() {
        super(BrightestDay.id("death_sense"), CorpsArsenal.exclusive(LanternCorps.BLACK));
    }

    @Override
    public int useCost() {
        return USE_COST;
    }

    @Override
    public int chargeTicks() {
        return CHARGE_TICKS;
    }

    @Override
    public void fire(ServerPlayer player, int size, int color) {
        long now = player.level().getGameTime();
        Long readyAt = READY_AT.get(player.getUUID());
        if (readyAt != null && now < readyAt) {
            PowerRingItem.refund(player, this.cost(size));
            player.sendOverlayMessage(Component.translatable("message.brightestday.undead.cooldown", (readyAt - now + 19) / 20).withColor(LanternCorps.BLACK.textColor()));
            return;
        }
        if (DeathSense.pulse(player) > 0) {
            READY_AT.put(player.getUUID(), now + COOLDOWN_TICKS);
            return;
        }
        PowerRingItem.refund(player, this.cost(size));
        player.sendOverlayMessage(Component.translatable("message.brightestday.death_sense.none", (int) DeathSense.RADIUS).withColor(LanternCorps.BLACK.textColor()));
    }
}
