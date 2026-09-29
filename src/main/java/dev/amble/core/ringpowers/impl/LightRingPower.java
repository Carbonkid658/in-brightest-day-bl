package dev.amble.core.ringpowers.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.amble.BrightestDay;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.ringpowers.RingPower;
import dev.amble.core.ringpowers.RingPowerRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class LightRingPower extends RingPower<LightRingPower.Data> {

    public static final String LAMB_DYNAMIC_LIGHTS = "lambdynlights";
    private static final int DRAIN_PER_SECOND = 3;

    public record Data(boolean on) {
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("on", false).forGetter(Data::on)
        ).apply(instance, Data::new));
    }

    public LightRingPower() {
        super(BrightestDay.id("light"), EnumSet.allOf(LanternCorps.class), Data.CODEC);
    }

    @Override
    public Data createData() {
        return new Data(false);
    }

    @Override
    public boolean run(ServerPlayer player, Data data) {
        return toggle(player);
    }

    @Override
    public int drainPerSecond(ServerPlayer player, Data data) {
        return isEmitting(player) ? DRAIN_PER_SECOND : 0;
    }

    @Override
    public void onDepleted(ServerPlayer player, Data data) {
        this.setData(player, new Data(false));
    }

    public static boolean isEmitting(Player player) {
        return ArmedRingPower.isArmed(player) && BrightestDayAttachments.get(player, RingPowerRegistry.LIGHT)
                .map(instance -> instance.data().on())
                .orElse(false);
    }

    public static boolean toggle(ServerPlayer player) {
        if (!ArmedRingPower.isArmed(player)) return false;

        return BrightestDayAttachments.get(player, RingPowerRegistry.LIGHT).map(instance -> {
            boolean on = !instance.data().on();
            BrightestDayAttachments.setData(player, RingPowerRegistry.LIGHT, new Data(on));
            player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                    on ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 1.8F);
            return true;
        }).orElse(false);
    }
}
