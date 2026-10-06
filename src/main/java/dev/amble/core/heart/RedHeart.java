package dev.amble.core.heart;

import dev.amble.core.progression.Milestone;
import dev.amble.core.progression.Trigger;
import dev.amble.core.progression.RingRanks;
import dev.amble.BrightestDay;
import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.LanternCorps;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RedHeart {
    public static final ResourceKey<DamageType> HEART_TORN = ResourceKey.create(Registries.DAMAGE_TYPE, BrightestDay.id("heart_torn"));

    private static final int RED = LanternCorps.RED.color();
    private static final int BLUE = LanternCorps.BLUE.color();
    private static final long RESET_TICKS = 20 * 60 * 5;
    private static final long DEBOUNCE_TICKS = 20;
    private static final long HOPE_TIMEOUT_TICKS = 20 * 30;
    private static final double HOPE_RANGE = 16.0;
    private static final float WOUND_DAMAGE = 12.0F;

    private static final Map<UUID, Attempts> ATTEMPTS = new HashMap<>();
    private static final Map<UUID, Plea> PLEAS = new HashMap<>();
    private static final Set<UUID> WARNED = new HashSet<>();

    private static final class Attempts {
        int count;
        long last;
    }

    private record Plea(UUID red, Set<UUID> blues, long expires) {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(RedHeart::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> WARNED.remove(handler.player.getUUID()));
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(
                Commands.literal(BrightestDay.MOD_ID).then(Commands.literal("hope").then(Commands.argument("red", UuidArgument.uuid())
                        .executes(context -> {
                            grantHope(context.getSource().getPlayerOrException(), UuidArgument.getUuid(context, "red"));
                            return 1;
                        })))));
    }

    public static boolean isBound(Player player, ItemStack ring) {
        return PowerRingItem.getCorps(ring).orElse(null) == LanternCorps.RED
                && ring == BrightestDayAttachments.getRing(player)
                && !player.hasInfiniteMaterials();
    }

    public static boolean mayRemove(Player player, ItemStack ring) {
        if (!isBound(player, ring)) return true;
        if (player instanceof ServerPlayer server) attempt(server);
        return false;
    }

    private static void attempt(ServerPlayer red) {
        long now = red.level().getGameTime();
        Attempts attempts = ATTEMPTS.computeIfAbsent(red.getUUID(), uuid -> new Attempts());
        if (now - attempts.last < DEBOUNCE_TICKS) return;
        if (now - attempts.last > RESET_TICKS) attempts.count = 0;
        attempts.last = now;

        if (PLEAS.containsKey(red.getUUID())) {
            red.sendOverlayMessage(Component.translatable("message.brightestday.red_heart.waiting").withColor(BLUE));
            return;
        }
        if (plead(red, now)) return;

        attempts.count++;
        ServerLevel level = red.level();
        switch (attempts.count) {
            case 1 -> {
                red.sendSystemMessage(Component.translatable("message.brightestday.red_heart.warning").withStyle(ChatFormatting.BOLD).withColor(RED));
                level.playSound(null, red.getX(), red.getY(), red.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.0F, 0.8F);
            }
            case 2 -> {
                red.hurtServer(level, level.damageSources().magic(), WOUND_DAMAGE);
                red.sendSystemMessage(Component.translatable("message.brightestday.red_heart.wound").withStyle(ChatFormatting.BOLD).withColor(RED));
                level.playSound(null, red.getX(), red.getY(), red.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 3.0F, 0.5F);
            }
            default -> tearOut(red);
        }
    }

    private static boolean plead(ServerPlayer red, long now) {
        Set<UUID> blues = new HashSet<>();
        for (ServerPlayer other : red.level().players()) {
            if (other != red && other.distanceToSqr(red) <= HOPE_RANGE * HOPE_RANGE
                    && PowerRingItem.getWornCorps(other).orElse(null) == LanternCorps.BLUE) blues.add(other.getUUID());
        }
        if (blues.isEmpty()) return false;

        PLEAS.put(red.getUUID(), new Plea(red.getUUID(), blues, now + HOPE_TIMEOUT_TICKS));
        Component button = Component.translatable("message.brightestday.red_heart.grant").withStyle(style -> style
                .withColor(BLUE).withBold(true).withUnderlined(true)
                .withClickEvent(new ClickEvent.RunCommand("/" + BrightestDay.MOD_ID + " hope " + red.getUUID()))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("message.brightestday.red_heart.grant_hover", red.getDisplayName()))));
        for (UUID uuid : blues) {
            ServerPlayer blue = red.level().getServer().getPlayerList().getPlayer(uuid);
            if (blue == null) continue;
            blue.sendSystemMessage(Component.translatable("message.brightestday.red_heart.plea", red.getDisplayName()).withColor(BLUE)
                    .append(" ").append(button));
        }
        red.sendSystemMessage(Component.translatable("message.brightestday.red_heart.hope_near").withStyle(ChatFormatting.ITALIC).withColor(BLUE));
        return true;
    }

    private static void grantHope(ServerPlayer blue, UUID redId) {
        Plea plea = PLEAS.get(redId);
        ServerPlayer red = blue.level().getServer().getPlayerList().getPlayer(redId);
        if (plea == null || red == null || !plea.blues().contains(blue.getUUID())) {
            blue.sendSystemMessage(Component.translatable("message.brightestday.red_heart.too_late").withColor(BLUE));
            return;
        }
        if (red.level() != blue.level() || red.distanceToSqr(blue) > HOPE_RANGE * HOPE_RANGE * 4) {
            blue.sendSystemMessage(Component.translatable("message.brightestday.red_heart.too_far").withColor(BLUE));
            return;
        }

        PLEAS.remove(redId);
        ATTEMPTS.remove(redId);
        ItemStack ring = BrightestDayAttachments.getRing(red);
        BrightestDayAttachments.setRing(red, ItemStack.EMPTY);
        if (!red.addItem(ring)) red.drop(ring, false, Prediction.SERVER_ONLY);

        Component message = Component.translatable("message.brightestday.red_heart.second_chance").withStyle(ChatFormatting.BOLD).withColor(BLUE);
        red.sendSystemMessage(message);
        blue.sendSystemMessage(message);
        RingRanks.fire(blue, Trigger.HOPE_GRANT, Milestone.Context.of(red));
        red.level().playSound(null, red.getX(), red.getY(), red.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5F, 1.4F);
    }

    private static void tearOut(ServerPlayer red) {
        ATTEMPTS.remove(red.getUUID());
        ItemStack ring = BrightestDayAttachments.getRing(red);
        BrightestDayAttachments.setRing(red, ItemStack.EMPTY);
        red.drop(ring, true, Prediction.SERVER_ONLY);

        ServerLevel level = red.level();
        level.playSound(null, red.getX(), red.getY(), red.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 4.0F, 0.3F);
        red.hurtServer(level, level.damageSources().source(HEART_TORN), Float.MAX_VALUE);
    }

    private static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        PLEAS.values().removeIf(plea -> {
            if (now < plea.expires()) return false;
            ServerPlayer red = server.getPlayerList().getPlayer(plea.red());
            if (red != null) red.sendSystemMessage(Component.translatable("message.brightestday.red_heart.no_answer").withStyle(ChatFormatting.ITALIC).withColor(RED));
            return true;
        });

        if (server.getTickCount() % 20 != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean red = PowerRingItem.getCorps(BrightestDayAttachments.getRing(player)).orElse(null) == LanternCorps.RED;
            if (!red) {
                WARNED.remove(player.getUUID());
            } else if (WARNED.add(player.getUUID())) {
                player.sendSystemMessage(Component.translatable("message.brightestday.red_heart.bound").withStyle(ChatFormatting.ITALIC).withColor(RED));
            }
        }
    }


    private RedHeart() {}
}
