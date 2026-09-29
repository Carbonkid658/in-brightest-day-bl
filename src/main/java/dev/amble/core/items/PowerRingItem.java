package dev.amble.core.items;

import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.BrightestDayComponents;
import dev.amble.core.ringpowers.CorpsSynergy;
import dev.amble.core.ringpowers.LanternCorps;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

public class PowerRingItem extends Item {
    public PowerRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack itemStack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        super.inventoryTick(itemStack, level, owner, slot);
        if (owner instanceof Player player && CorpsSynergy.empoweredByHope(player)) return;
        PowerRingItem.tickCharge(itemStack, level);
    }

    public static boolean tickCharge(ItemStack ring, ServerLevel level) {
        if (level.getServer().getTickCount() % (20 * 60) * 20 != 0) return false;

        int before = getRingPower(ring);
        drainRing(ring, 1);
        return getRingPower(ring) != before;
    }

    public static Optional<LanternCorps> getCorps(ItemStack ring) {
        if (!(ring.getItem() instanceof PowerRingItem)) return Optional.empty();
        return Optional.ofNullable(ring.get(BrightestDayComponents.LANTERN_CORPS));
    }

    public static ItemStack getWornRing(Player player) {
        ItemStack slotted = BrightestDayAttachments.getRing(player);
        if (slotted.getItem() instanceof PowerRingItem) return slotted;
        if (player.getMainHandItem().getItem() instanceof PowerRingItem) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof PowerRingItem) return player.getOffhandItem();
        return ItemStack.EMPTY;
    }

    public static Optional<LanternCorps> getWornCorps(Player player) {
        return getCorps(getWornRing(player));
    }

    public static float getChargeFraction(ItemStack ring) {
        return (float) getRingPower(ring) / BrightestDayComponents.MAX_POWER;
    }

    public static void refund(Player player, int amount) {
        amount = CorpsSynergy.scaleCost(player, amount);
        ItemStack ring = getWornRing(player);
        if (ring.isEmpty() || player.hasInfiniteMaterials()) return;

        chargeRing(ring, amount);
        if (ring == BrightestDayAttachments.getRing(player)) BrightestDayAttachments.setRing(player, ring);
    }

    public static boolean hasCharge(Player player) {
        return player.hasInfiniteMaterials() || getRingPower(getWornRing(player)) > 0;
    }

    public static boolean drainWorn(Player player, int amount) {
        amount = CorpsSynergy.scaleCost(player, amount);
        ItemStack ring = getWornRing(player);
        if (ring.isEmpty()) return false;

        boolean paid = getRingPower(ring) >= amount;
        drainRing(ring, amount);
        if (ring == BrightestDayAttachments.getRing(player)) BrightestDayAttachments.setRing(player, ring);
        return paid;
    }

    public static boolean consumeCharge(Player player, int amount) {
        amount = CorpsSynergy.scaleCost(player, amount);
        ItemStack ring = getWornRing(player);
        if (ring.isEmpty() || getRingPower(ring) < amount) return false;

        drainRing(ring, amount);
        if (ring == BrightestDayAttachments.getRing(player)) BrightestDayAttachments.setRing(player, ring);
        return true;
    }

    public static int getRingPower(ItemStack ring) {
        if (!(ring.getItem() instanceof PowerRingItem)) return 0;
        return ring.getOrDefault(BrightestDayComponents.POWER_TYPE, 0);
    }

    public static void drainRing(ItemStack ring, int amount) {
        if (!(ring.getItem() instanceof PowerRingItem)) return;
        int current = ring.getOrDefault(BrightestDayComponents.POWER_TYPE, 0);
        if (current <= 0) return;

        ring.set(BrightestDayComponents.POWER_TYPE, Math.max(current - amount, 0));
    }

    public static void chargeRing(ItemStack ring, int amount) {
        if (!(ring.getItem() instanceof PowerRingItem)) return;
        int current = ring.getOrDefault(BrightestDayComponents.POWER_TYPE, BrightestDayComponents.MAX_POWER);
        if (current >= BrightestDayComponents.MAX_POWER) return;

        ring.set(BrightestDayComponents.POWER_TYPE, Math.min(current + amount, BrightestDayComponents.MAX_POWER));
    }

    public static void setMaxPower(ItemStack ring) {
        if (!(ring.getItem() instanceof PowerRingItem)) return;
        ring.set(BrightestDayComponents.POWER_TYPE, BrightestDayComponents.MAX_POWER);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);

        double percentage = ((double) PowerRingItem.getRingPower(itemStack) / BrightestDayComponents.MAX_POWER) * 100;

        int color = PowerRingItem.getCorps(itemStack).orElse(LanternCorps.GREEN).color();
        Component component = Component.literal(String.format("%.0f%%", percentage)).withStyle(ChatFormatting.BOLD).withColor(color);

        builder.accept(component);
    }
}
