package dev.amble.core.mannequin;

import dev.amble.BrightestDay;
import dev.amble.core.BrightestDayItems;
import dev.amble.core.menus.MannequinMenu;
import dev.amble.mixin.MannequinAccessor;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public final class Mannequins {
    public static final float PAD_HEIGHT = 2.0F / 16.0F;
    private static final double EDIT_REACH = 8.0;

    public static final AttachmentType<Hologram> HOLOGRAM =
            AttachmentRegistry.<Hologram>builder()
                    .persistent(Hologram.CODEC)
                    .syncWith(ByteBufCodecs.fromCodecWithRegistries(Hologram.CODEC), AttachmentSyncPredicate.all())
                    .buildAndRegister(BrightestDay.id("hologram"));

    public static void init() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || !(entity instanceof Mannequin mannequin) || !isHologram(mannequin) || player.isSpectator()) return InteractionResult.PASS;
            if (!mayEdit(mannequin, player)) {
                player.sendOverlayMessage(Component.translatable("message.brightestday.mannequin.locked").withStyle(ChatFormatting.GRAY));
                return InteractionResult.FAIL;
            }
            if (player instanceof ServerPlayer server) open(server, mannequin);
            return InteractionResult.CONSUME;
        });
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!(entity instanceof Mannequin mannequin) || !isHologram(mannequin)) return InteractionResult.PASS;
            if (!mayEdit(mannequin, player)) {
                player.sendOverlayMessage(Component.translatable("message.brightestday.mannequin.locked").withStyle(ChatFormatting.GRAY));
                return InteractionResult.FAIL;
            }
            if (level instanceof ServerLevel server) dismantle(server, mannequin, player);
            return InteractionResult.SUCCESS;
        });
    }

    public static boolean isHologram(@Nullable Entity entity) {
        return entity instanceof Mannequin && entity.hasAttached(HOLOGRAM);
    }

    public static Hologram hologram(Entity entity) {
        return entity.getAttachedOrElse(HOLOGRAM, Hologram.EMPTY);
    }

    public static void update(Mannequin mannequin, Hologram hologram) {
        mannequin.setAttached(HOLOGRAM, hologram);
    }

    public static boolean mayEdit(Mannequin mannequin, Player player) {
        Hologram hologram = hologram(mannequin);
        return !hologram.settings().locked() || hologram.ownedBy(player.getUUID());
    }

    public static @Nullable Mannequin place(ServerLevel level, Vec3 at, float yaw, ServerPlayer owner) {
        Mannequin mannequin = Mannequin.create(EntityTypes.MANNEQUIN, level);
        if (mannequin == null) return null;
        float snapped = Math.round(Mth.wrapDegrees(yaw) / 45.0F) * 45.0F;
        mannequin.snapTo(at.x, at.y + PAD_HEIGHT, at.z, snapped, 0.0F);
        mannequin.setNoGravity(true);
        mannequin.setPermanentlyInvulnerable(true);
        ((MannequinAccessor) mannequin).brightestday$setImmovable(true);
        HologramSettings settings = HologramSettings.DEFAULT.withYaw(snapped);
        mannequin.setAttached(HOLOGRAM, new Hologram(ItemStack.EMPTY, Optional.of(owner.getUUID()), settings));
        apply(mannequin, null, settings);
        if (!level.addFreshEntity(mannequin)) return null;
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 1.6F);
        return mannequin;
    }

    public static void edit(ServerPlayer player, Mannequin mannequin, HologramSettings requested) {
        if (!isHologram(mannequin) || player.distanceTo(mannequin) > EDIT_REACH || !mayEdit(mannequin, player)) return;
        Hologram hologram = hologram(mannequin);
        HologramSettings next = requested.sanitized();
        if (!hologram.ownedBy(player.getUUID())) next = next.withLocked(hologram.settings().locked());
        HologramSettings previous = hologram.settings();
        update(mannequin, hologram.withSettings(next));
        apply(mannequin, previous, next);
    }

    private static void apply(Mannequin mannequin, @Nullable HologramSettings previous, HologramSettings next) {
        if (previous == null || !previous.skin().equals(next.skin())) {
            mannequin.setComponent(DataComponents.PROFILE, next.skin().isEmpty() ? Mannequin.DEFAULT_PROFILE : ResolvableProfile.createUnresolved(next.skin()));
        }
        if (previous != null && previous.pad() != next.pad()) {
            mannequin.setPos(mannequin.getX(), mannequin.getY() + (next.pad() ? PAD_HEIGHT : -PAD_HEIGHT), mannequin.getZ());
        }
        ((MannequinAccessor) mannequin).brightestday$setHideDescription(true);
        mannequin.setMainArm(next.leftHanded() ? HumanoidArm.LEFT : HumanoidArm.RIGHT);
        mannequin.setCustomName(next.skin().isEmpty() ? null : Component.literal(next.skin()));
        mannequin.setCustomNameVisible(!next.hideName() && !next.skin().isEmpty());
        mannequin.setYRot(next.yaw());
        mannequin.setYBodyRot(next.yaw());
        mannequin.setYHeadRot(next.yaw());
        mannequin.yRotO = next.yaw();
        mannequin.yBodyRotO = next.yaw();
        mannequin.yHeadRotO = next.yaw();
    }

    private static void open(ServerPlayer player, Mannequin mannequin) {
        player.openMenu(new ExtendedMenuProvider<Integer>() {
            @Override
            public Integer getScreenOpeningData(ServerPlayer opener) {
                return mannequin.getId();
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable("container.brightestday.mannequin");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player opener) {
                return new MannequinMenu(containerId, inventory, mannequin.getId(), new HologramContainer(mannequin));
            }
        });
    }

    private static void dismantle(ServerLevel level, Mannequin mannequin, Player player) {
        Hologram hologram = hologram(mannequin);
        if (!player.hasInfiniteMaterials()) mannequin.spawnAtLocation(level, new ItemStack(BrightestDayItems.LANTERN_MANNEQUIN));
        if (!hologram.ring().isEmpty()) mannequin.spawnAtLocation(level, hologram.ring().copy());
        ItemStack lantern = mannequin.getItemBySlot(EquipmentSlot.OFFHAND);
        if (!lantern.isEmpty()) mannequin.spawnAtLocation(level, lantern.copy());
        mannequin.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        level.playSound(null, mannequin.getX(), mannequin.getY(), mannequin.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.8F, 1.6F);
        mannequin.discard();
    }

    private Mannequins() {}
}
