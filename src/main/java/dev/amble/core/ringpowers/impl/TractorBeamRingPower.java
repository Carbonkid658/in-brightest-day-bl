package dev.amble.core.ringpowers.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.amble.BrightestDay;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.ringpowers.RingPower;
import dev.amble.core.ringpowers.RingPowerCategory;
import dev.amble.core.ringpowers.RingPowerRegistry;
import dev.amble.core.tractor.TractorManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class TractorBeamRingPower extends RingPower<TractorBeamRingPower.Data> {

    public record Data(boolean active) {
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("active", false).forGetter(Data::active)
        ).apply(instance, Data::new));
    }

    public TractorBeamRingPower() {
        super(BrightestDay.id("tractor_beam"), EnumSet.allOf(LanternCorps.class), Data.CODEC);
    }

    @Override
    public RingPowerCategory category() {
        return RingPowerCategory.CONSTRUCT;
    }

    @Override
    public boolean slotted() {
        return true;
    }

    @Override
    public Data createData() {
        return new Data(false);
    }

    @Override
    public boolean run(ServerPlayer player, Data data) {
        boolean active = !data.active();
        if (active && !ArmedRingPower.isArmed(player)) {
            player.sendOverlayMessage(Component.translatable("message.brightestday.raise_ring_first"));
            return false;
        }
        this.setData(player, new Data(active));
        if (!active) TractorManager.release(player);
        return true;
    }

    @Override
    public void onDepleted(ServerPlayer player, Data data) {
        if (data.active()) this.setData(player, new Data(false));
        TractorManager.release(player);
    }

    @Override
    public void onRevoked(ServerPlayer player, Data data) {
        TractorManager.release(player);
    }

    public static boolean isActive(Player player) {
        return BrightestDayAttachments.get(player, RingPowerRegistry.TRACTOR_BEAM)
                .map(instance -> instance.data().active())
                .orElse(false);
    }
}
