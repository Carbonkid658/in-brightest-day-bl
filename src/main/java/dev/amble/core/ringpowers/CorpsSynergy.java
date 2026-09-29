package dev.amble.core.ringpowers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.amble.BrightestDay;
import dev.amble.core.items.PowerRingItem;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class CorpsSynergy {
    private static final int CHECK_INTERVAL = 10;
    private static final double LINK_ENTER_RADIUS = 48.0;
    private static final double LINK_EXIT_RADIUS = 60.0;
    private static final double DREAD_ENTER_RADIUS = 16.0;
    private static final double DREAD_EXIT_RADIUS = 20.0;
    private static final float HOPE_COST_MULTIPLIER = 0.25F;
    public static final float HOPE_BUBBLE_SCALE = 1.5F;
    private static final float DREAD_COST_MULTIPLIER = 2.0F;
    private static final float DREAD_SIZE_SCALE = 0.6F;
    public static final float DREAD_BUBBLE_SCALE = 0.75F;

    public record State(boolean hope, boolean will, boolean dread) {
        public static final State NONE = new State(false, false, false);

        public static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("hope", false).forGetter(State::hope),
                Codec.BOOL.optionalFieldOf("will", false).forGetter(State::will),
                Codec.BOOL.optionalFieldOf("dread", false).forGetter(State::dread)
        ).apply(instance, State::new));

        public static final StreamCodec<ByteBuf, State> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, State::hope,
                ByteBufCodecs.BOOL, State::will,
                ByteBufCodecs.BOOL, State::dread,
                State::new
        );
    }

    public static final AttachmentType<State> SYNERGY =
            AttachmentRegistry.<State>builder()
                    .initializer(() -> State.NONE)
                    .syncWith(State.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
                    .buildAndRegister(BrightestDay.id("corps_synergy"));

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(CorpsSynergy::tick);
    }

    public static State get(Player player) {
        return player.getAttachedOrElse(SYNERGY, State.NONE);
    }

    public static boolean empoweredByHope(Player player) {
        return get(player).hope();
    }

    public static boolean borrowsWill(Player player) {
        return get(player).will();
    }

    public static boolean weakenedByHope(Player player) {
        return get(player).dread();
    }

    public static int scaleCost(Player player, int amount) {
        if (amount <= 0) return amount;
        if (empoweredByHope(player)) return Math.max(1, Math.round(amount * HOPE_COST_MULTIPLIER));
        if (weakenedByHope(player)) return Math.round(amount * DREAD_COST_MULTIPLIER);
        return amount;
    }

    public static int weakenSize(int minSize, int maxSize) {
        return Math.max(minSize, Math.round(maxSize * DREAD_SIZE_SCALE));
    }

    public static LanternCorps powerCorps(Player player, LanternCorps corps) {
        return corps == LanternCorps.BLUE && borrowsWill(player) ? LanternCorps.GREEN : corps;
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % CHECK_INTERVAL != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            State previous = get(player);
            LanternCorps corps = PowerRingItem.hasCharge(player) ? PowerRingItem.getWornCorps(player).orElse(null) : null;

            boolean hope = corps == LanternCorps.GREEN && isNear(player, LanternCorps.BLUE, previous.hope() ? LINK_EXIT_RADIUS : LINK_ENTER_RADIUS);
            boolean will = corps == LanternCorps.BLUE && isNear(player, LanternCorps.GREEN, previous.will() ? LINK_EXIT_RADIUS : LINK_ENTER_RADIUS);
            boolean dread = (corps == LanternCorps.YELLOW || corps == LanternCorps.RED)
                    && isNear(player, LanternCorps.BLUE, previous.dread() ? DREAD_EXIT_RADIUS : DREAD_ENTER_RADIUS);
            State next = new State(hope, will, dread);
            if (next.equals(previous)) continue;

            player.setAttached(SYNERGY, next);
            if (hope != previous.hope()) {
                player.sendOverlayMessage(Component.translatable(hope ? "message.brightestday.hope_gained" : "message.brightestday.hope_lost")
                        .withColor(LanternCorps.BLUE.color()));
            }
            if (will != previous.will()) {
                player.sendOverlayMessage(Component.translatable(will ? "message.brightestday.will_gained" : "message.brightestday.will_lost")
                        .withColor(LanternCorps.GREEN.color()));
            }
            if (dread != previous.dread()) {
                player.sendOverlayMessage(Component.translatable(dread ? "message.brightestday.dread_gained" : "message.brightestday.dread_lost")
                        .withColor(LanternCorps.BLUE.color()));
            }
        }
    }

    private static boolean isNear(ServerPlayer player, LanternCorps corps, double radius) {
        for (ServerPlayer other : player.level().players()) {
            if (other == player || other.isSpectator() || other.distanceToSqr(player) > radius * radius) continue;
            if (PowerRingItem.hasCharge(other) && PowerRingItem.getWornCorps(other).orElse(null) == corps) return true;
        }
        return false;
    }

    private CorpsSynergy() {}
}
