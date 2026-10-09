package dev.amble.client.team;

import dev.amble.core.networking.payloads.c2s.TeamActionC2SPayload;
import dev.amble.core.networking.payloads.s2c.TeamInvitesS2CPayload;
import dev.amble.core.networking.payloads.s2c.TeamRosterS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.LanternCorps;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ClientTeams {
    private static final Set<UUID> INCOMING = new HashSet<>();
    private static final Set<UUID> OUTGOING = new HashSet<>();
    private static List<TeamRosterS2CPayload.Member> roster = List.of();

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(TeamInvitesS2CPayload.TYPE, (payload, context) -> {
            for (UUID inviter : payload.incoming()) {
                if (!INCOMING.contains(inviter)) announce(context.client(), inviter);
            }
            INCOMING.clear();
            INCOMING.addAll(payload.incoming());
            OUTGOING.clear();
            OUTGOING.addAll(payload.outgoing());
        });
        ClientPlayNetworking.registerGlobalReceiver(TeamRosterS2CPayload.TYPE, (payload, context) -> roster = List.copyOf(payload.members()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            INCOMING.clear();
            OUTGOING.clear();
            roster = List.of();
        });
    }

    private static void announce(Minecraft client, UUID inviter) {
        Player player = client.level != null ? client.level.getPlayerByUUID(inviter) : null;
        Component name = player != null ? player.getDisplayName() : Component.translatable("toast.brightestday.team_invite.someone");
        LanternCorps corps = player != null ? PowerRingItem.getWornCorps(player).orElse(null) : null;
        client.gui.toastManager().addToast(new TeamInviteToast(inviter, name, corps));
    }

    public static List<TeamRosterS2CPayload.Member> roster() {
        return roster;
    }

    public static boolean invitedBy(UUID player) {
        return INCOMING.contains(player);
    }

    public static boolean invited(UUID player) {
        return OUTGOING.contains(player);
    }

    public static void send(TeamActionC2SPayload.Action action, UUID target) {
        if (action == TeamActionC2SPayload.Action.INVITE) OUTGOING.add(target);
        if (action == TeamActionC2SPayload.Action.CANCEL) OUTGOING.remove(target);
        if (action == TeamActionC2SPayload.Action.ACCEPT || action == TeamActionC2SPayload.Action.DECLINE) INCOMING.remove(target);
        ClientPlayNetworking.send(new TeamActionC2SPayload(action, target));
    }

    private ClientTeams() {}
}
