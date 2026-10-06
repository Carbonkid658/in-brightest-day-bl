package dev.amble.core.ringpowers.impl;

import com.mojang.serialization.MapCodec;
import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.ringpowers.RingPower;
import dev.amble.core.ringpowers.RingPowerCategory;
import net.minecraft.util.Unit;

import java.util.EnumSet;

public class CommsRingPower extends RingPower<Unit> {
    public CommsRingPower() {
        super(BrightestDay.id("comms"), EnumSet.allOf(LanternCorps.class), MapCodec.unitCodec(Unit.INSTANCE));
    }

    @Override
    public RingPowerCategory category() {
        return RingPowerCategory.UTILITY;
    }

    @Override
    public Unit createData() {
        return Unit.INSTANCE;
    }
}
