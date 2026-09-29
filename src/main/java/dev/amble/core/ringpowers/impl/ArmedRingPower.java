package dev.amble.core.ringpowers.impl;

import dev.amble.core.ringpowers.CorpsColors;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.amble.BrightestDay;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.ringpowers.RingPower;
import dev.amble.core.ringpowers.RingPowerCategory;
import dev.amble.core.ringpowers.RingPowerInstance;
import dev.amble.core.ringpowers.RingPowerRegistry;
import dev.amble.core.ringpowers.constructs.ConstructRingPower;
import dev.amble.core.beams.BeamManager;
import dev.amble.core.beams.HealBeamManager;
import dev.amble.core.tractor.TractorManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public class ArmedRingPower extends RingPower<ArmedRingPower.Data> {

    public static final int COOLDOWN_TICKS = 10;

    private static final Map<ServerPlayer, Long> LAST_FIRED = new WeakHashMap<>();

    public record Data(boolean active, Optional<Identifier> construct) {
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("active", false).forGetter(Data::active),
                Identifier.CODEC.optionalFieldOf("construct").forGetter(Data::construct)
        ).apply(instance, Data::new));

        public Data withActive(boolean active) {
            return new Data(active, this.construct);
        }

        public Data withConstruct(Identifier construct) {
            return new Data(this.active, Optional.of(construct));
        }
    }

    public ArmedRingPower() {
        super(BrightestDay.id("armed"), EnumSet.allOf(LanternCorps.class), Data.CODEC);
    }

    @Override
    public RingPowerCategory category() {
        return RingPowerCategory.STANCE;
    }

    @Override
    public Data createData() {
        return new Data(false, Optional.empty());
    }

    @Override
    public boolean worksWithoutCharge() {
        return true;
    }

    @Override
    public boolean run(ServerPlayer player, Data data) {
        if (data.active()) {
            lower(player);
        } else {
            this.setData(player, data.withActive(true));
        }
        return true;
    }

    public static void lower(ServerPlayer player) {
        BeamManager.stop(player);
        HealBeamManager.stop(player);
        data(player).ifPresent(data -> BrightestDayAttachments.setData(player, RingPowerRegistry.ARMED, data.withActive(false)));
        BrightestDayAttachments.setData(player, RingPowerRegistry.TRACTOR_BEAM, new TractorBeamRingPower.Data(false));
        TractorManager.release(player);
    }

    public static boolean isArmed(Player player) {
        return data(player).map(Data::active).orElse(false);
    }

    public static Optional<ConstructRingPower> selectedConstruct(Player player) {
        List<ConstructRingPower> constructs = constructs(player);
        if (constructs.isEmpty()) return Optional.empty();

        Optional<Identifier> selected = data(player).flatMap(Data::construct);
        return Optional.of(constructs.stream()
                .filter(construct -> selected.isPresent() && construct.id().equals(selected.get()))
                .findFirst()
                .orElse(constructs.getFirst()));
    }

    public static void cycle(ServerPlayer player) {
        if (!PowerRingItem.hasCharge(player)) {
            player.sendOverlayMessage(Component.translatable("message.brightestday.ring_depleted"));
            return;
        }

        List<ConstructRingPower> constructs = constructs(player);
        Optional<Data> data = data(player);
        if (constructs.isEmpty() || data.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.brightestday.no_constructs"));
            return;
        }

        ConstructRingPower current = selectedConstruct(player).orElse(constructs.getFirst());
        ConstructRingPower next = constructs.get((constructs.indexOf(current) + 1) % constructs.size());
        BrightestDayAttachments.setData(player, RingPowerRegistry.ARMED, data.get().withConstruct(next.id()));
        player.sendOverlayMessage(Component.translatable("message.brightestday.construct_selected", Component.translatable(next.getTranslationKey())));
    }

    public static void fire(ServerPlayer player, int radius) {
        if (!isArmed(player) || player.isSpectator()) return;

        ServerLevel level = player.level();
        Optional<ConstructRingPower> construct = selectedConstruct(player);
        if (construct.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.brightestday.no_constructs"));
            return;
        }

        long now = level.getGameTime();
        Long last = LAST_FIRED.get(player);
        if (last != null && now - last < COOLDOWN_TICKS) return;

        if (!player.hasInfiniteMaterials() && !PowerRingItem.consumeCharge(player, construct.get().cost(radius))) {
            level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 1.6F);
            return;
        }
        LAST_FIRED.put(player, now);

        int color = CorpsColors.of(player);
        construct.get().fire(player, radius, color);
    }

    private static Optional<Data> data(Player player) {
        return BrightestDayAttachments.get(player, RingPowerRegistry.ARMED).map(RingPowerInstance::data);
    }

    private static List<ConstructRingPower> constructs(Player player) {
        return BrightestDayAttachments.get(player).stream()
                .map(RingPowerInstance::power)
                .filter(ConstructRingPower.class::isInstance)
                .map(ConstructRingPower.class::cast)
                .toList();
    }
}
