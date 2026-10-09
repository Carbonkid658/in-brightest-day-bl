package dev.amble.core.ringpowers.constructs;

import dev.amble.BrightestDay;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.CorpsArsenal;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.ringpowers.UndeadControl;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommandDeadConstruct extends ConstructRingPower {
    private static final int USE_COST = 60;
    private static final int CHARGE_TICKS = 15;
    private static final long COOLDOWN_TICKS = 15L * 20L;

    private static final Map<UUID, Long> READY_AT = new HashMap<>();

    public CommandDeadConstruct() {
        super(BrightestDay.id("command_dead"), CorpsArsenal.exclusive(LanternCorps.BLACK));
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
    public void fire(ServerPlayer player, int radius, int color) {
        long now = player.level().getGameTime();
        Long readyAt = READY_AT.get(player.getUUID());
        if (!player.isShiftKeyDown() && readyAt != null && now < readyAt) {
            PowerRingItem.refund(player, this.cost(radius));
            player.sendOverlayMessage(Component.translatable("message.brightestday.undead.cooldown", (readyAt - now + 19) / 20).withColor(UndeadControl.TEXT_COLOR));
            return;
        }
        int result = UndeadControl.command(player, color);
        if (result > 0) {
            READY_AT.put(player.getUUID(), now + COOLDOWN_TICKS);
            player.sendOverlayMessage(Component.translatable("message.brightestday.undead.raised", result, UndeadControl.controlledCount(player)).withColor(UndeadControl.TEXT_COLOR));
            return;
        }
        PowerRingItem.refund(player, this.cost(radius));
        if (result == 0) player.sendOverlayMessage(Component.translatable("message.brightestday.undead.none", (int) UndeadControl.RADIUS).withColor(UndeadControl.TEXT_COLOR));
    }
}
