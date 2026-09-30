package dev.amble.core.ringpowers.impl;

import com.mojang.serialization.MapCodec;
import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.ringpowers.RingPower;
import dev.amble.core.ringpowers.RingPowerCategory;
import net.minecraft.util.Unit;

import java.util.EnumSet;

public class AcidRingPower extends RingPower<Unit> {

    public AcidRingPower() {
        super(BrightestDay.id("acid_vomit"), EnumSet.of(LanternCorps.RED), MapCodec.unitCodec(Unit.INSTANCE));
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
