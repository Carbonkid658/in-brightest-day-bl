package dev.amble.core.ringpowers.constructs;

import dev.amble.config.BrightestDayConfig;
import dev.amble.BrightestDay;
import dev.amble.core.drill.DrillGeometry;
import dev.amble.core.drill.DrillManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class DrillConstruct extends ConstructRingPower {

    public DrillConstruct() {
        super(BrightestDay.id("drill"));
    }

    @Override
    public int useCost() {
        return BrightestDayConfig.get().drillCost;
    }

    @Override
    public int chargeTicks() {
        return BrightestDayConfig.get().drillChargeTicks;
    }

    @Override
    public boolean usesSize() {
        return true;
    }

    @Override
    public int maxSize() {
        return 3;
    }

    @Override
    public int empoweredMaxSize() {
        return 4;
    }

    @Override
    public Component describeSize(int size) {
        int side = DrillGeometry.side(this.costSize(size));
        return Component.literal(side + "×" + side);
    }

    @Override
    public boolean sustained() {
        return true;
    }

    @Override
    public void fire(ServerPlayer player, int size, int color) {
        DrillManager.start(player, this.clampSize(player, size), color);
    }
}
