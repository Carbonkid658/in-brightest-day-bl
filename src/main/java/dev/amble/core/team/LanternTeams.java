package dev.amble.core.team;

import dev.amble.BrightestDay;
import dev.amble.config.BrightestDayConfig;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.networking.payloads.s2c.TeamInvitesS2CPayload;
import dev.amble.core.ringpowers.LanternCorps;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class LanternTeams {
    private static final int INVITE_TICKS = 20 * 60;
    private static final int EXPIRY_CHECK_INTERVAL = 20;

    public static final AttachmentType<UUID> TEAM =
            AttachmentRegistry.<UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .copyOnDeath()
                    .syncWith(UUIDUtil.STREAM_CODEC, AttachmentSyncPredicate.all())
                    .buildAndRegister(BrightestDay.id("lantern_team"));

    private static final Map<UUID, Map<UUID, Integer>> INVITES = new HashMap<>();

    public static void init() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(source.is(RingDamage.RING_CONSTRUCT) && areTeammates(entity, source.getEntity())));
        ServerTickEvents.END_SERVER_TICK.register(LanternTeams::expireInvites);
        ServerPlayerEvents.LEAVE.register(LanternTeams::forgetInvites);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> INVITES.clear());
    }

    public static Optional<UUID> team(Player player) {
        return Optional.ofNullable(player.getAttached(TEAM));
    }

    public static boolean areTeammates(@Nullable Entity first, @Nullable Entity second) {
        if (!(first instanceof Player a) || !(second instanceof Player b) || a == b) return false;
        UUID team = a.getAttached(TEAM);
        return team != null && team.equals(b.getAttached(TEAM));
    }

    public static List<ServerPlayer> members(MinecraftServer server, UUID team) {
        List<ServerPlayer> members = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (team.equals(player.getAttached(TEAM))) members.add(player);
        }
        return members;
    }

    public static boolean canInvite(Player inviter, Player invitee) {
        return inviter != invitee
                && !invitee.isSpectator()
                && inviter.level() == invitee.level()
                && inviter.distanceTo(invitee) <= BrightestDayConfig.get().teamInviteRange
                && PowerRingItem.getWornCorps(inviter).isPresent()
                && PowerRingItem.getWornCorps(invitee).isPresent()
                && !areTeammates(inviter, invitee);
    }

    public static void invite(ServerPlayer inviter, ServerPlayer invitee) {
        if (!canInvite(inviter, invitee)) return;
        if (teamSize(inviter) >= BrightestDayConfig.get().teamMaxSize) {
            inviter.sendOverlayMessage(Component.translatable("message.brightestday.team.full"));
            return;
        }

        int now = inviter.level().getServer().getTickCount();
        INVITES.computeIfAbsent(invitee.getUUID(), id -> new HashMap<>()).put(inviter.getUUID(), now + INVITE_TICKS);
        inviter.sendOverlayMessage(Component.translatable("message.brightestday.team.invite_sent", invitee.getDisplayName()));
        sync(inviter);
        sync(invitee);
    }

    public static void cancel(ServerPlayer inviter, UUID invitee) {
        Map<UUID, Integer> pending = INVITES.get(invitee);
        if (pending == null || pending.remove(inviter.getUUID()) == null) return;
        if (pending.isEmpty()) INVITES.remove(invitee);
        sync(inviter);
        ServerPlayer target = inviter.level().getServer().getPlayerList().getPlayer(invitee);
        if (target != null) sync(target);
    }

    public static void accept(ServerPlayer invitee, UUID inviterId) {
        Map<UUID, Integer> pending = INVITES.get(invitee.getUUID());
        if (pending == null || !pending.containsKey(inviterId)) return;

        MinecraftServer server = invitee.level().getServer();
        ServerPlayer inviter = server.getPlayerList().getPlayer(inviterId);
        pending.remove(inviterId);
        if (pending.isEmpty()) INVITES.remove(invitee.getUUID());
        if (inviter == null) {
            sync(invitee);
            return;
        }
        if (teamSize(inviter) >= BrightestDayConfig.get().teamMaxSize) {
            invitee.sendOverlayMessage(Component.translatable("message.brightestday.team.full"));
            sync(invitee);
            sync(inviter);
            return;
        }

        leave(invitee, false);
        UUID team = team(inviter).orElseGet(() -> {
            UUID created = UUID.randomUUID();
            inviter.setAttached(TEAM, created);
            return created;
        });
        invitee.setAttached(TEAM, team);

        Component joined = Component.translatable("message.brightestday.team.joined", invitee.getDisplayName(), corpsName(invitee))
                .withColor(corpsColor(invitee));
        for (ServerPlayer member : members(server, team)) {
            member.sendSystemMessage(joined);
            sync(member);
        }
    }

    public static void decline(ServerPlayer invitee, UUID inviterId) {
        Map<UUID, Integer> pending = INVITES.get(invitee.getUUID());
        if (pending == null || pending.remove(inviterId) == null) return;
        if (pending.isEmpty()) INVITES.remove(invitee.getUUID());
        sync(invitee);

        ServerPlayer inviter = invitee.level().getServer().getPlayerList().getPlayer(inviterId);
        if (inviter == null) return;
        inviter.sendOverlayMessage(Component.translatable("message.brightestday.team.declined", invitee.getDisplayName()));
        sync(inviter);
    }

    public static void leave(ServerPlayer player, boolean announce) {
        UUID team = player.getAttached(TEAM);
        if (team == null) return;

        player.removeAttached(TEAM);
        List<ServerPlayer> remaining = members(player.level().getServer(), team);
        if (remaining.size() == 1) remaining.getFirst().removeAttached(TEAM);
        if (!announce) return;

        Component left = Component.translatable("message.brightestday.team.left", player.getDisplayName());
        player.sendSystemMessage(Component.translatable("message.brightestday.team.you_left"));
        for (ServerPlayer member : remaining) member.sendSystemMessage(left);
    }

    private static int teamSize(ServerPlayer player) {
        return team(player).map(team -> members(player.level().getServer(), team).size()).orElse(1);
    }

    private static void expireInvites(MinecraftServer server) {
        if (server.getTickCount() % EXPIRY_CHECK_INTERVAL != 0 || INVITES.isEmpty()) return;

        int now = server.getTickCount();
        List<UUID> touched = new ArrayList<>();
        INVITES.entrySet().removeIf(entry -> {
            boolean changed = entry.getValue().entrySet().removeIf(invite -> {
                boolean expired = invite.getValue() <= now;
                if (expired) touched.add(invite.getKey());
                return expired;
            });
            if (changed) touched.add(entry.getKey());
            return entry.getValue().isEmpty();
        });
        touched.stream().distinct().map(server.getPlayerList()::getPlayer).filter(Objects::nonNull).forEach(LanternTeams::sync);
    }

    private static void forgetInvites(ServerPlayer player) {
        UUID id = player.getUUID();
        INVITES.remove(id);
        List<UUID> touched = new ArrayList<>();
        INVITES.entrySet().removeIf(entry -> {
            if (entry.getValue().remove(id) != null) touched.add(entry.getKey());
            return entry.getValue().isEmpty();
        });
        touched.stream().map(player.level().getServer().getPlayerList()::getPlayer).filter(Objects::nonNull).forEach(LanternTeams::sync);
    }

    private static void sync(ServerPlayer player) {
        UUID id = player.getUUID();
        List<UUID> incoming = List.copyOf(INVITES.getOrDefault(id, Map.of()).keySet());
        List<UUID> outgoing = new ArrayList<>();
        INVITES.forEach((invitee, inviters) -> {
            if (inviters.containsKey(id)) outgoing.add(invitee);
        });
        ServerPlayNetworking.send(player, new TeamInvitesS2CPayload(incoming, outgoing));
    }

    private static Component corpsName(Player player) {
        return PowerRingItem.getWornCorps(player).map(LanternCorps::displayName).orElse(Component.empty());
    }

    private static int corpsColor(Player player) {
        return PowerRingItem.getWornCorps(player).map(LanternCorps::color).orElse(0xFFFFFF);
    }

    private LanternTeams() {}
}
