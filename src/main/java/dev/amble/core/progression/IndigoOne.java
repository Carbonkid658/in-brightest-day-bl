package dev.amble.core.progression;

import dev.amble.config.BrightestDayConfig;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.BrightestDayBlocks;
import dev.amble.core.BrightestDayComponents;
import dev.amble.core.BrightestDayItems;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.LanternCorps;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class IndigoOne {
    private static final int CHANNEL_TICKS = 100;
    private static final double CHANNEL_RANGE = 5.0;
    private static final int INDIGO = LanternCorps.INDIGO.color();

    private record Channel(UUID target, long ends, float indigoHealth, float targetHealth) {}

    private static final Map<UUID, Channel> CHANNELS = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(IndigoOne::tick);
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer indigo) || !(entity instanceof ServerPlayer target)) return InteractionResult.PASS;
            return embrace(indigo, target) ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
        });
    }

    public static boolean multiplayer(MinecraftServer server) {
        return server.isDedicatedServer() || server.isPublished();
    }

    public static boolean isIndigoOne(Player player) {
        MinecraftServer server = player.level().getServer();
        return server != null && WorldProgress.get(server).indigoOne().map(player.getUUID()::equals).orElse(false);
    }

    private static boolean embrace(ServerPlayer indigo, ServerPlayer target) {
        if (!isIndigoOne(indigo) || !indigo.getMainHandItem().isEmpty()) return false;
        if (PowerRingItem.getCorps(BrightestDayAttachments.getRing(indigo)).orElse(null) != LanternCorps.INDIGO) return false;

        LanternCorps corps = PowerRingItem.getCorps(BrightestDayAttachments.getRing(target)).orElse(null);
        if (corps == null || corps == LanternCorps.INDIGO) return false;
        if (corps == LanternCorps.RED) {
            indigo.sendOverlayMessage(Component.translatable("message.brightestday.indigo.red_heart").withColor(INDIGO));
            return true;
        }
        if (SpectrumMeters.get(target, Emotion.COMPASSION) < Emotion.GATE) {
            indigo.sendOverlayMessage(Component.translatable("message.brightestday.indigo.closed", target.getDisplayName()).withColor(INDIGO));
            return true;
        }
        if (CHANNELS.containsKey(indigo.getUUID())) return true;

        CHANNELS.put(indigo.getUUID(), new Channel(target.getUUID(), indigo.level().getGameTime() + CHANNEL_TICKS, indigo.getHealth(), target.getHealth()));
        target.sendSystemMessage(Component.translatable("message.brightestday.indigo.embracing", indigo.getDisplayName()).withStyle(ChatFormatting.ITALIC).withColor(INDIGO));
        indigo.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.5F, 0.7F);
        return true;
    }

    private static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        CHANNELS.entrySet().removeIf(entry -> {
            ServerPlayer indigo = server.getPlayerList().getPlayer(entry.getKey());
            Channel channel = entry.getValue();
            ServerPlayer target = server.getPlayerList().getPlayer(channel.target());
            if (indigo == null || target == null || indigo.level() != target.level() || indigo.distanceTo(target) > CHANNEL_RANGE
                    || indigo.getHealth() < channel.indigoHealth() || target.getHealth() < channel.targetHealth()) {
                if (indigo != null) indigo.sendOverlayMessage(Component.translatable("message.brightestday.indigo.broken").withColor(INDIGO));
                return true;
            }

            Vec3 from = indigo.getEyePosition();
            Vec3 to = target.getBoundingBox().getCenter();
            for (int i = 0; i < 6; i++) {
                Vec3 point = from.lerp(to, indigo.getRandom().nextDouble());
                indigo.level().sendParticles(new DustParticleOptions(INDIGO, 1.2F), point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.0);
            }
            if (now < channel.ends()) return false;

            convert(indigo, target);
            return true;
        });

        if (server.getTickCount() % 100 != 0 || !multiplayer(server) || WorldProgress.get(server).indigoOne().isPresent()) return;
        List<ServerPlayer> online = server.getPlayerList().getPlayers();
        if (online.size() < BrightestDayConfig.get().indigoMinPlayers) return;
        List<ServerPlayer> ringless = online.stream().filter(player -> BrightestDayAttachments.getRing(player).isEmpty() && !player.isSpectator()).toList();
        if (ringless.isEmpty()) return;

        ServerPlayer chosen = ringless.get(server.overworld().getRandom().nextInt(ringless.size()));
        WorldProgress.update(server, state -> state.withIndigoOne(chosen.getUUID()));
        chosen.sendSystemMessage(Component.translatable("message.brightestday.indigo.chosen").withStyle(ChatFormatting.BOLD).withColor(INDIGO));
        RingOffers.bestow(chosen, LanternCorps.INDIGO);
    }

    private static void convert(ServerPlayer indigo, ServerPlayer target) {
        ItemStack old = BrightestDayAttachments.getRing(target);
        float charge = PowerRingItem.getChargeFraction(old);
        ItemStack ring = new ItemStack(BrightestDayItems.ring(LanternCorps.INDIGO));
        ring.set(BrightestDayComponents.POWER_TYPE, Math.round(RingRanks.capacity(target, LanternCorps.INDIGO) * charge));
        BrightestDayAttachments.setRing(target, ring);
        BrightestDayBlocks.lantern(LanternCorps.INDIGO).ifPresent(lantern -> {
            ItemStack gift = new ItemStack(lantern);
            if (!target.addItem(gift)) target.drop(gift, false, Prediction.SERVER_ONLY);
        });

        Component message = Component.translatable("message.brightestday.indigo.converted", target.getDisplayName()).withStyle(ChatFormatting.BOLD).withColor(INDIGO);
        indigo.sendSystemMessage(message);
        target.sendSystemMessage(message);
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5F, 0.6F);
        RingRanks.complete(indigo, RankTask.INDIGO_CONVERT);
    }

    private IndigoOne() {}
}
