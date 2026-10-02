package dev.amble.core.ringpowers.constructs;

import dev.amble.config.BrightestDayConfig;
import dev.amble.BrightestDay;
import dev.amble.core.drill.DrillGeometry;
import dev.amble.core.drill.DrillManager;
import dev.amble.core.drill.PlacedDrillManager;
import dev.amble.core.items.PowerRingItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

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
    public boolean sustained(Player player) {
        return !player.isShiftKeyDown();
    }

    @Override
    public void fire(ServerPlayer player, int size, int color) {
        if (!player.isShiftKeyDown()) {
            DrillManager.start(player, this.clampSize(player, size), color);
            return;
        }
        if (PlacedDrillManager.place(player, this.clampSize(player, size), color)) return;
        PowerRingItem.refund(player, this.cost(size));
        player.sendOverlayMessage(Component.translatable("message.brightestday.drill_place_blocked"));
    }
}
