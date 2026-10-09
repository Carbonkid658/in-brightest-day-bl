package dev.amble.core.ringpowers.constructs;

import dev.amble.BrightestDay;
import dev.amble.config.BrightestDayConfig;
import dev.amble.core.attacks.projectile.PlasmaManager;
import dev.amble.core.ringpowers.CorpsArsenal;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import net.minecraft.server.level.ServerPlayer;

public class PlasmaBurstConstruct extends ConstructRingPower {
    public PlasmaBurstConstruct() {
        super(BrightestDay.id("plasma_burst"), CorpsArsenal.exclusive(LanternCorps.RED));
    }

    @Override
    public int useCost() {
        return BrightestDayConfig.get().plasmaCost;
    }

    @Override
    public int chargeTicks() {
        return BrightestDayConfig.get().plasmaChargeTicks;
    }

    @Override
    public int releaseTicks() {
        return BrightestDayConfig.get().plasmaChargeTicks / 4;
    }

    @Override
    public void fire(ServerPlayer player, int size, int color) {
        PlasmaManager.launch(player, color, ArmedRingPower.chargeFraction(player, this.chargeTicks()));
    }
}
