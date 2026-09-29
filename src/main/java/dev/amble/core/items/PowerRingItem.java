package dev.amble.core.items;

import dev.amble.core.BrightestDayComponents;
import dev.amble.core.blocks.GreenLanternBlock;
import dev.amble.core.ringpowers.LanternCorps;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

public class PowerRingItem extends Item {
    public PowerRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Block block = context.getLevel().getBlockState(context.getClickedPos()).getBlock();
        if (block instanceof GreenLanternBlock) {
            if (!(context.getLevel() instanceof ServerLevel)) return InteractionResult.CONSUME;
            if (PowerRingItem.getRingPower(context.getItemInHand()) >= BrightestDayComponents.MAX_POWER) {
                return InteractionResult.CONSUME;
            }

            PowerRingItem.setMaxPower(context.getItemInHand());
            Component oath = Component.literal("""
                    In brightest day,
                    In blackest night,
                    No evil shall escape my sight.
                    Let those who worship evil's might,
                    Beware my power,
                    Green Lantern's light!""")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
            context.getPlayer().sendSystemMessage(oath);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void inventoryTick(ItemStack itemStack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        super.inventoryTick(itemStack, level, owner, slot);
        if (level.getServer().getTickCount() % (20 * 60) * 20 == 0) {
            PowerRingItem.drainRing(itemStack, 1);
        }
    }

    public static Optional<LanternCorps> getCorps(ItemStack ring) {
        if (!(ring.getItem() instanceof PowerRingItem)) return Optional.empty();
        return Optional.ofNullable(ring.get(BrightestDayComponents.LANTERN_CORPS));
    }

    public static Optional<LanternCorps> getWornCorps(Player player) {
        return getCorps(player.getMainHandItem()).or(() -> getCorps(player.getOffhandItem()));
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
