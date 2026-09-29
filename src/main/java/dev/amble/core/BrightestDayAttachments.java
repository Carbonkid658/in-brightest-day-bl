package dev.amble.core;

import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.ringpowers.RingPower;
import dev.amble.core.ringpowers.RingPowerInstance;
import dev.amble.core.ringpowers.RingPowerRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BrightestDayAttachments {
    public static final AttachmentType<List<RingPowerInstance<?>>> POWERS =
            AttachmentRegistry.<List<RingPowerInstance<?>>>builder()
                    .initializer(List::of)
                    .persistent(RingPowerInstance.CODEC.listOf())
                    .copyOnDeath()
                    .syncWith(RingPowerInstance.LIST_STREAM_CODEC, AttachmentSyncPredicate.all())
                    .buildAndRegister(BrightestDay.id("powers"));

    public static final AttachmentType<ItemStack> RING =
            AttachmentRegistry.<ItemStack>builder()
                    .initializer(() -> ItemStack.EMPTY)
                    .persistent(ItemStack.OPTIONAL_CODEC)
                    .copyOnDeath()
                    .syncWith(ItemStack.OPTIONAL_STREAM_CODEC, AttachmentSyncPredicate.all())
                    .buildAndRegister(BrightestDay.id("ring"));

    public static final int MAX_SLOTS = 4;

    public static ItemStack getRing(Player player) {
        return player.getAttachedOrElse(BrightestDayAttachments.RING, ItemStack.EMPTY);
    }

    public static void setRing(Player player, ItemStack ring) {
        player.setAttached(BrightestDayAttachments.RING, ring.isEmpty() ? ItemStack.EMPTY : ring.copy());
    }

    public static List<RingPowerInstance<?>> get(Player player) {
        return player.getAttachedOrElse(BrightestDayAttachments.POWERS, List.of());
    }

    @SuppressWarnings("unchecked")
    public static <D> Optional<RingPowerInstance<D>> get(Player player, RingPower<D> power) {
        for (RingPowerInstance<?> instance : get(player)) {
            if (instance.is(power)) return Optional.of((RingPowerInstance<D>) instance);
        }
        return Optional.empty();
    }

    public static boolean has(Player player, RingPower<?> power) {
        return get(player, power).isPresent();
    }

    public static <D> void setData(Player player, RingPower<D> power, D data) {
        List<RingPowerInstance<?>> current = get(player);
        List<RingPowerInstance<?>> updated = new ArrayList<>(current.size());
        boolean found = false;
        for (RingPowerInstance<?> instance : current) {
            if (instance.is(power)) {
                updated.add(new RingPowerInstance<>(power, data));
                found = true;
            } else {
                updated.add(instance);
            }
        }
        if (found) player.setAttached(BrightestDayAttachments.POWERS, List.copyOf(updated));
    }

    public static void sync(ServerPlayer player, @Nullable LanternCorps corps) {
        List<RingPowerInstance<?>> current = get(player);
        List<RingPower<?>> available = corps == null
                ? List.of()
                : RingPowerRegistry.forCorps(corps).stream().limit(MAX_SLOTS).toList();

        if (current.size() == available.size()) {
            boolean unchanged = true;
            for (int i = 0; i < current.size() && unchanged; i++) {
                unchanged = current.get(i).is(available.get(i));
            }
            if (unchanged) return;
        }

        List<RingPowerInstance<?>> updated = new ArrayList<>(available.size());
        List<RingPowerInstance<?>> granted = new ArrayList<>();
        for (RingPower<?> power : available) {
            RingPowerInstance<?> instance = get(player, power).orElse(null);
            if (instance == null) {
                instance = power.createInstance();
                granted.add(instance);
            }
            updated.add(instance);
        }

        player.setAttached(BrightestDayAttachments.POWERS, List.copyOf(updated));

        for (RingPowerInstance<?> instance : current) {
            if (!updated.contains(instance)) revoked(player, instance);
        }
        for (RingPowerInstance<?> instance : granted) {
            granted(player, instance);
        }
    }

    private static <D> void granted(ServerPlayer player, RingPowerInstance<D> instance) {
        instance.power().onGranted(player, instance.data());
    }

    private static <D> void revoked(ServerPlayer player, RingPowerInstance<D> instance) {
        instance.power().onRevoked(player, instance.data());
    }

    public static void init() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayer player)) return;
            if (player.level().getGameRules().get(GameRules.KEEP_INVENTORY)) return;

            ItemStack ring = getRing(player);
            if (ring.isEmpty()) return;

            player.spawnAtLocation(player.level(), ring);
            setRing(player, ItemStack.EMPTY);
        });
    }
}
