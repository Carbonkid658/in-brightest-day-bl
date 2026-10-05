package dev.amble.client.team;

import dev.amble.core.networking.payloads.c2s.TeamActionC2SPayload;
import dev.amble.core.networking.payloads.s2c.TeamInvitesS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ClientTeams {
    private static final Set<UUID> INCOMING = new HashSet<>();
    private static final Set<UUID> OUTGOING = new HashSet<>();

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(TeamInvitesS2CPayload.TYPE, (payload, context) -> {
            INCOMING.clear();
            INCOMING.addAll(payload.incoming());
            OUTGOING.clear();
            OUTGOING.addAll(payload.outgoing());
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            INCOMING.clear();
            OUTGOING.clear();
        });
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
