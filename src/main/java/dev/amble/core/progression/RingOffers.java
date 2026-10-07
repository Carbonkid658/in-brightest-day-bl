package dev.amble.core.progression;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.amble.core.loyalty.RingBonds;
import dev.amble.BrightestDay;
import dev.amble.config.BrightestDayConfig;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.BrightestDayComponents;
import dev.amble.core.BrightestDayItems;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.loyalty.RingLoyalty;
import dev.amble.core.networking.payloads.s2c.TintFlashS2CPayload;
import dev.amble.core.ringpowers.LanternCorps;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

public final class RingOffers {
    private static final long OFFER_TICKS = 20 * 60;
    private static final long RED_COOLDOWN = 24000;
    private static final int MINUTES_PER_DAY = 20;
    private static final int DUEL_WARNING_TICKS = 200;
    private static final long DUEL_TICKS = 20 * 60 * 5;
    private static final int FLASH_TICKS = 60;

    private record Offer(LanternCorps corps, long expires) {}

    private record Challenge(UUID challenger, UUID holder, long teleportAt, long expires) {}

    private static final Map<UUID, Offer> OFFERS = new HashMap<>();
    private static final Map<UUID, Long> RED_REFUSED = new HashMap<>();
    private static final Map<UUID, Challenge> CHALLENGES = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(RingOffers::tick);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer victim && source.getEntity() instanceof ServerPlayer killer) settleDuel(victim, killer);
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(
                Commands.literal(BrightestDay.MOD_ID).then(Commands.literal("offer").then(Commands.argument("answer", StringArgumentType.word())
                        .executes(context -> {
                            answer(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "answer").equals("accept"));
                            return 1;
                        })))));
    }

    public static ItemStack forge(LanternCorps corps, Player bearer) {
        ItemStack ring = new ItemStack(BrightestDayItems.ring(corps));
        ring.set(BrightestDayComponents.POWER_TYPE, RingRanks.capacity(bearer, corps));
        return ring;
    }

    public static void bestow(ServerPlayer player, LanternCorps corps) {
        RingLoyalty.deliver(player, forge(corps, player));
    }

    private static boolean ringless(Player player) {
        return BrightestDayAttachments.getRing(player).isEmpty() && !player.isSpectator()
                && !(player instanceof ServerPlayer server && RingBonds.bonded(server));
    }

    private static void offer(ServerPlayer player, LanternCorps corps, String key) {
        if (absent(player.level().getServer(), corps) || corps != LanternCorps.ORANGE && !CorpsCaps.admits(player, corps)) return;
        OFFERS.put(player.getUUID(), new Offer(corps, player.level().getGameTime() + OFFER_TICKS));
        MutableComponent accept = button("accept", corps.color());
        MutableComponent refuse = button("refuse", 0xAAAAAA);
        player.sendSystemMessage(Component.translatable("message.brightestday.offer." + key).withStyle(ChatFormatting.ITALIC).withColor(corps.color())
                .append(" ").append(accept).append(" ").append(refuse));
    }

    public static void forceOffer(ServerPlayer player, LanternCorps corps) {
        switch (corps) {
            case RED -> offer(player, corps, "red");
            case ORANGE -> offer(player, corps, "orange");
            default -> bestow(player, corps);
        }
    }

    private static MutableComponent button(String answer, int color) {
        return Component.translatable("message.brightestday.offer." + answer).withStyle(style -> style
                .withColor(color).withBold(true).withUnderlined(true)
                .withClickEvent(new ClickEvent.RunCommand("/" + BrightestDay.MOD_ID + " offer " + answer)));
    }

    private static void answer(ServerPlayer player, boolean accepted) {
        Offer offer = OFFERS.remove(player.getUUID());
        if (offer == null || player.level().getGameTime() > offer.expires()) {
            player.sendSystemMessage(Component.translatable("message.brightestday.offer.expired").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!accepted) {
            if (offer.corps() == LanternCorps.RED) RED_REFUSED.put(player.getUUID(), player.level().getGameTime());
            player.sendSystemMessage(Component.translatable("message.brightestday.offer.refused").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!ringless(player)) return;

        if (absent(player.level().getServer(), offer.corps())) {
            player.sendSystemMessage(Component.translatable("message.brightestday.offer.absent").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (offer.corps() == LanternCorps.ORANGE && !CorpsCaps.admits(player, LanternCorps.ORANGE)) {
            ServerPlayer holder = orangeHolder(player.level().getServer());
            if (holder != null) challenge(player, holder);
            else CorpsCaps.refuse(player, LanternCorps.ORANGE);
            return;
        }
        if (!CorpsCaps.check(player, offer.corps())) return;
        bestow(player, offer.corps());
    }

    public static void considerOrange(ServerPlayer player) {
        if (!ringless(player) || OFFERS.containsKey(player.getUUID()) || CHALLENGES.containsKey(player.getUUID())) return;
        if (absent(player.level().getServer(), LanternCorps.ORANGE)) return;
        float chance = Math.min(0.5F, SpectrumMeters.get(player, Emotion.AVARICE) / 2000.0F);
        if (player.getRandom().nextFloat() >= chance) return;
        offer(player, LanternCorps.ORANGE, "orange");
    }

    public static void considerGreen(ServerPlayer player) {
        if (!ringless(player) || RingLoyalty.inFlight(LanternCorps.GREEN) || !CorpsCaps.admits(player, LanternCorps.GREEN)) return;
        MinecraftServer server = player.level().getServer();
        BrightestDayConfig config = BrightestDayConfig.get();
        if (SpectrumMeters.get(player, Emotion.WILL) < config.greenRingWill) return;

        int online = server.getPlayerList().getPlayerCount();
        int slots = Math.max(1, online / Math.max(1, config.greenPlayersPerRing));
        int bearers = CorpsCaps.bearers(server, LanternCorps.GREEN).size();
        if (bearers >= slots) return;

        player.sendSystemMessage(Component.translatable("message.brightestday.offer.green").withStyle(ChatFormatting.ITALIC).withColor(LanternCorps.GREEN.color()));
        bestow(player, LanternCorps.GREEN);
    }

    public static boolean absent(MinecraftServer server, LanternCorps corps) {
        if (corps != LanternCorps.ORANGE) return false;
        List<UUID> bearers = CorpsCaps.bearers(server, corps);
        return bearers.size() >= CorpsCaps.cap(corps) && bearers.stream().noneMatch(id -> server.getPlayerList().getPlayer(id) != null);
    }


    private static ServerPlayer orangeHolder(MinecraftServer server) {
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (PowerRingItem.getCorps(BrightestDayAttachments.getRing(other)).orElse(null) == LanternCorps.ORANGE) return other;
        }
        return null;
    }

    private static void challenge(ServerPlayer challenger, ServerPlayer holder) {
        long now = challenger.level().getGameTime();
        CHALLENGES.put(challenger.getUUID(), new Challenge(challenger.getUUID(), holder.getUUID(), now + DUEL_WARNING_TICKS, now + DUEL_WARNING_TICKS + DUEL_TICKS));
        holder.sendSystemMessage(Component.translatable("message.brightestday.duel.warning", challenger.getDisplayName())
                .withStyle(ChatFormatting.BOLD).withColor(LanternCorps.ORANGE.color()));
        challenger.sendSystemMessage(Component.translatable("message.brightestday.duel.summoned", holder.getDisplayName())
                .withStyle(ChatFormatting.BOLD).withColor(LanternCorps.ORANGE.color()));
    }

    private static void settleDuel(ServerPlayer victim, ServerPlayer killer) {
        Challenge challenge = CHALLENGES.get(killer.getUUID());
        if (challenge != null && challenge.holder().equals(victim.getUUID())) {
            CHALLENGES.remove(killer.getUUID());
            ItemStack ring = BrightestDayAttachments.getRing(victim);
            if (PowerRingItem.getCorps(ring).orElse(null) != LanternCorps.ORANGE || !ringless(killer)) return;
            BrightestDayAttachments.setRing(victim, ItemStack.EMPTY);
            RingLoyalty.deliver(killer, ring);
            killer.sendSystemMessage(Component.translatable("message.brightestday.duel.won").withStyle(ChatFormatting.BOLD).withColor(LanternCorps.ORANGE.color()));
            return;
        }
        CHALLENGES.values().removeIf(other -> other.challenger().equals(victim.getUUID()) && other.holder().equals(killer.getUUID()));
    }

    private static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        OFFERS.values().removeIf(offer -> now > offer.expires());
        CHALLENGES.values().removeIf(challenge -> {
            if (now > challenge.expires()) return true;
            if (now != challenge.teleportAt()) return false;
            ServerPlayer challenger = server.getPlayerList().getPlayer(challenge.challenger());
            ServerPlayer holder = server.getPlayerList().getPlayer(challenge.holder());
            if (challenger == null || holder == null) return true;
            challenger.teleportTo(holder.level(), holder.getX(), holder.getY(), holder.getZ(), Set.of(), challenger.getYRot(), challenger.getXRot(), true);
            holder.level().playSound(null, holder.getX(), holder.getY(), holder.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5F, 0.6F);
            return false;
        });

        if (server.getTickCount() % 1200 != 0) return;
        BrightestDayConfig config = BrightestDayConfig.get();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!ringless(player)) continue;
            considerGreen(player);
            if (OFFERS.containsKey(player.getUUID()) || SpectrumMeters.get(player, Emotion.RAGE) < config.redOfferRage) continue;
            Long refused = RED_REFUSED.get(player.getUUID());
            if (refused != null && now - refused < RED_COOLDOWN) continue;
            if (player.getRandom().nextFloat() >= config.redOfferDailyChance / MINUTES_PER_DAY) continue;

            ServerPlayNetworking.send(player, new TintFlashS2CPayload(LanternCorps.RED.color(), FLASH_TICKS));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.0F, 0.6F);
            RED_REFUSED.put(player.getUUID(), now);
            offer(player, LanternCorps.RED, "red");
        }
    }

    private RingOffers() {}
}
