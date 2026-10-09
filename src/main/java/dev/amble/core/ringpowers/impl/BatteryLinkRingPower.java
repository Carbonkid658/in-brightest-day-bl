package dev.amble.core.ringpowers.impl;

import com.mojang.serialization.MapCodec;
import dev.amble.BrightestDay;
import dev.amble.core.forge.CentralPowerBattery;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.ringpowers.RingPower;
import dev.amble.core.ringpowers.RingPowerCategory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;

import java.util.EnumSet;

/**
 * The Black Lantern's way of feeding at a Black Central Power Battery: stand near it, use this ability,
 * and speak the oath to recharge the ring and bind it to you. Works even when the ring is empty.
 */
//public class BatteryLinkRingPower extends RingPower<Unit> {
//
//    public BatteryLinkRingPower() {
//        super(BrightestDay.id("battery_link"), EnumSet.of(LanternCorps.BLACK), MapCodec.unitCodec(Unit.INSTANCE));
//    }
//
//    @Override
//    public RingPowerCategory category() {
//        return RingPowerCategory.UTILITY;
//    }
//
//    @Override
//    public boolean worksWithoutCharge() {
//        return true;
//    }
//
//    @Override
//    public Unit createData() {
//        return Unit.INSTANCE;
//    }
//
//    public static void fire(ServerPlayer player) {
//        CentralPowerBattery.link(player);
//    }
//}
