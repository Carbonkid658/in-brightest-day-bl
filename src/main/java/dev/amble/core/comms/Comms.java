package dev.amble.core.comms;

import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.networking.payloads.s2c.CommsIncomingS2CPayload;
import dev.amble.core.networking.payloads.s2c.CommsTalkingS2CPayload;
import dev.amble.core.networking.payloads.s2c.CommsTargetS2CPayload;
import dev.amble.core.ringpowers.RingPowerRegistry;
import dev.amble.core.ringpowers.impl.ArmedRingPower;
import dev.amble.core.team.LanternTeams;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class Comms {
    public static final String VOICE_CHAT_MOD_ID = "voicechat";
    private static final int CHECK_INTERVAL = 10;

    private static final Map<UUID, UUID> DIALED = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> TRANSMITTING = new ConcurrentHashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(Comms::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.player.getUUID();
            stop(server, id);
            DIALED.remove(id);
        });
        EntityTrackingEvents.START_TRACKING.register((entity, watcher) -> {
            if (entity instanceof ServerPlayer player) ServerPlayNetworking.send(watcher, new CommsTalkingS2CPayload(player.getId(), TRANSMITTING.containsKey(player.getUUID())));
        });
    }

    private static volatile Predicate<UUID> voiceReady = id -> false;

    public static void setVoiceReady(Predicate<UUID> check) {
        voiceReady = check;
    }

    public static boolean voiceReady(UUID player) {
        return available() && voiceReady.test(player);
    }

    public static boolean available() {
        return FabricLoader.getInstance().isModLoaded(VOICE_CHAT_MOD_ID);
    }

    public static @Nullable UUID receiver(UUID sender) {
        return TRANSMITTING.get(sender);
    }

    public static void cycle(ServerPlayer player, int direction) {
        if (!BrightestDayAttachments.has(player, RingPowerRegistry.COMMS)) return;
        List<ServerPlayer> teammates = teammates(player);
        if (teammates.isEmpty()) {
            DIALED.remove(player.getUUID());
            ServerPlayNetworking.send(player, CommsTargetS2CPayload.NONE);
            return;
        }

        UUID current = DIALED.get(player.getUUID());
        int index = -1;
        for (int i = 0; i < teammates.size(); i++) {
            if (teammates.get(i).getUUID().equals(current)) index = i;
        }
        int next = index < 0 ? (direction >= 0 ? 0 : teammates.size() - 1) : Math.floorMod(index + direction, teammates.size());
        ServerPlayer target = teammates.get(next);
        DIALED.put(player.getUUID(), target.getUUID());
        ServerPlayNetworking.send(player, new CommsTargetS2CPayload(target.getScoreboardName()));

        UUID transmitting = TRANSMITTING.get(player.getUUID());
        if (transmitting != null && !transmitting.equals(target.getUUID())) {
            stop(player.level().getServer(), player.getUUID());
            start(player);
        }
    }

    public static void start(ServerPlayer player) {
        if (!available() || !BrightestDayAttachments.has(player, RingPowerRegistry.COMMS) || ArmedRingPower.activeAbility(player).orElse(null) != RingPowerRegistry.COMMS) return;
        ServerPlayer target = dialed(player);
        if (target == null) {
            cycle(player, 1);
            target = dialed(player);
            if (target == null) return;
        }
        if (target.getUUID().equals(TRANSMITTING.get(player.getUUID()))) return;

        TRANSMITTING.put(player.getUUID(), target.getUUID());
        ServerPlayNetworking.send(target, new CommsIncomingS2CPayload(player.getScoreboardName(), true));
        broadcastTalking(player, true);
    }

    public static void stop(MinecraftServer server, UUID sender) {
        UUID target = TRANSMITTING.remove(sender);
        if (target == null) return;
        ServerPlayer receiver = server.getPlayerList().getPlayer(target);
        ServerPlayer player = server.getPlayerList().getPlayer(sender);
        if (receiver != null) {
            ServerPlayNetworking.send(receiver, new CommsIncomingS2CPayload(player != null ? player.getScoreboardName() : "", false));
        }
        if (player != null) broadcastTalking(player, false);
    }

    private static @Nullable ServerPlayer dialed(ServerPlayer player) {
        UUID id = DIALED.get(player.getUUID());
        if (id == null) return null;
        ServerPlayer target = player.level().getServer().getPlayerList().getPlayer(id);
        return target != null && LanternTeams.areTeammates(player, target) ? target : null;
    }

    private static List<ServerPlayer> teammates(ServerPlayer player) {
        List<ServerPlayer> teammates = new ArrayList<>();
        LanternTeams.team(player).ifPresent(team -> {
            for (ServerPlayer member : LanternTeams.members(player.level().getServer(), team)) {
                if (member != player) teammates.add(member);
            }
        });
        teammates.sort(Comparator.comparing(member -> member.getScoreboardName()));
        return teammates;
    }

    private static void broadcastTalking(ServerPlayer player, boolean talking) {
        CommsTalkingS2CPayload payload = new CommsTalkingS2CPayload(player.getId(), talking);
        ServerPlayNetworking.send(player, payload);
        for (ServerPlayer watcher : PlayerLookup.tracking(player)) {
            if (watcher != player) ServerPlayNetworking.send(watcher, payload);
        }
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % CHECK_INTERVAL != 0 || TRANSMITTING.isEmpty()) return;
        for (UUID sender : List.copyOf(TRANSMITTING.keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(sender);
            if (player == null || !BrightestDayAttachments.has(player, RingPowerRegistry.COMMS) || dialed(player) == null) stop(server, sender);
        }
    }

    private Comms() {}
}
