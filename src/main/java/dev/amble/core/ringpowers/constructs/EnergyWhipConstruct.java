package dev.amble.core.ringpowers.constructs;

import dev.amble.BrightestDay;
import dev.amble.config.BrightestDayConfig;
import dev.amble.core.attacks.weapon.WhipManager;
import net.minecraft.server.level.ServerPlayer;

public class EnergyWhipConstruct extends ConstructRingPower {
    public EnergyWhipConstruct() {
        super(BrightestDay.id("energy_whip"));
    }

    @Override
    public int useCost() {
        return BrightestDayConfig.get().whipCost;
    }

    @Override
    public int chargeTicks() {
        return BrightestDayConfig.get().whipChargeTicks;
    }

    @Override
    public void fire(ServerPlayer player, int size, int color) {
        WhipManager.crack(player, color);
    }
}
