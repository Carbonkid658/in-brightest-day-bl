package dev.amble.core.ringpowers.impl;

import com.mojang.serialization.MapCodec;
import dev.amble.BrightestDay;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.ringpowers.RedRage;
import dev.amble.core.ringpowers.RingPower;
import dev.amble.core.ringpowers.RingPowerCategory;
import dev.amble.core.ringpowers.RingPowerRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;

import java.util.EnumSet;

public class BerserkRingPower extends RingPower<Unit> {
    public BerserkRingPower() {
        super(BrightestDay.id("berserk"), EnumSet.of(LanternCorps.RED), MapCodec.unitCodec(Unit.INSTANCE));
    }

    @Override
    public RingPowerCategory category() {
        return RingPowerCategory.UTILITY;
    }

    @Override
    public Unit createData() {
        return Unit.INSTANCE;
    }

    public static void fire(ServerPlayer player) {
        if (player.isSpectator() || !BrightestDayAttachments.has(player, RingPowerRegistry.BERSERK)) return;
        RedRage.berserk(player);
    }
}
