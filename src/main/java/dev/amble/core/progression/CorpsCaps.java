package dev.amble.core.progression;

import com.mojang.serialization.Codec;
import dev.amble.BrightestDay;
import dev.amble.config.BrightestDayConfig;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.BrightestDayComponents;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.LanternCorps;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CorpsCaps {
    private static final int TRACK_INTERVAL = 20;

    public static final AttachmentType<Map<LanternCorps, List<UUID>>> ROSTER =
            AttachmentRegistry.<Map<LanternCorps, List<UUID>>>builder()
                    .initializer(Map::of)
                    .persistent(Codec.unboundedMap(LanternCorps.CODEC, UUIDUtil.CODEC.listOf()))
                    .buildAndRegister(BrightestDay.id("corps_roster"));

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(CorpsCaps::tick);
    }

    public static int cap(LanternCorps corps) {
        BrightestDayConfig config = BrightestDayConfig.get();
        return switch (corps) {
            case GREEN -> config.greenCap;
            case YELLOW -> config.yellowCap;
            case RED -> config.redCap;
            case BLUE -> config.blueCap;
            case INDIGO -> config.indigoCap;
            case STAR_SAPPHIRE -> config.starSapphireCap;
            default -> Integer.MAX_VALUE;
        };
    }

    public static boolean admits(Player player, LanternCorps corps) {
        if (!(player instanceof ServerPlayer server) || player.hasInfiniteMaterials()) return true;
        List<UUID> bearers = roster(server.level().getServer()).getOrDefault(corps, List.of());
        return bearers.contains(player.getUUID()) || bearers.size() < cap(corps);
    }

    public static boolean admits(Player player, ItemStack ring) {
        return PowerRingItem.getCorps(ring).map(corps -> admits(player, corps)).orElse(true);
    }

    public static boolean check(Player player, LanternCorps corps) {
        if (admits(player, corps)) return true;
        refuse(player, corps);
        return false;
    }

    public static boolean check(Player player, ItemStack ring) {
        return PowerRingItem.getCorps(ring).map(corps -> check(player, corps)).orElse(true);
    }

    public static void refuse(Player player, LanternCorps corps) {
        player.sendOverlayMessage(Component.translatable("message.brightestday.corps_full", corps.displayName()).withColor(corps.color()));
    }

    private static Map<LanternCorps, List<UUID>> roster(MinecraftServer server) {
        return server.overworld().getAttachedOrElse(ROSTER, Map.of());
    }

    private static Set<LanternCorps> held(ServerPlayer player) {
        Set<LanternCorps> held = EnumSet.noneOf(LanternCorps.class);
        ItemStack worn = BrightestDayAttachments.getRing(player);
        if (!PowerRingItem.isDormant(worn)) PowerRingItem.getCorps(worn).ifPresent(held::add);
        for (ItemStack stack : player.getInventory()) {
            BrightestDayComponents.Sworn sworn = stack.get(BrightestDayComponents.SWORN_TO);
            if (sworn == null || !sworn.owner().equals(player.getUUID()) || PowerRingItem.isDormant(stack)) continue;
            PowerRingItem.getCorps(stack).ifPresent(held::add);
        }
        return held;
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % TRACK_INTERVAL != 0) return;
        Map<LanternCorps, List<UUID>> current = roster(server);
        Map<LanternCorps, List<UUID>> next = new EnumMap<>(LanternCorps.class);
        current.forEach((corps, bearers) -> next.put(corps, new ArrayList<>(bearers)));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Set<LanternCorps> held = held(player);
            for (LanternCorps corps : LanternCorps.values()) {
                List<UUID> bearers = next.computeIfAbsent(corps, key -> new ArrayList<>());
                boolean listed = bearers.contains(player.getUUID());
                if (held.contains(corps) && !listed) bearers.add(player.getUUID());
                else if (!held.contains(corps) && listed) bearers.remove(player.getUUID());
            }
        }
        next.values().removeIf(List::isEmpty);
        if (!next.equals(current)) server.overworld().setAttached(ROSTER, Map.copyOf(next));
    }

    private CorpsCaps() {}
}
