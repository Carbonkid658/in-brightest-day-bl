package dev.amble.core.ringpowers.constructs;

import dev.amble.core.BrightestDayComponents;
import dev.amble.core.items.PowerRingItem;
import dev.amble.core.ringpowers.CorpsColors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;

public final class ConstructTools {
    public static final int LIFETIME_TICKS = 1200;
    private static final int DRAIN_PER_SECOND = 2;

    public static void init() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof ItemEntity item && isConstruct(item.getItem())) {
                level.playSound(null, item.getX(), item.getY(), item.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.6F, 1.6F);
                item.discard();
            }
        });
    }

    public static boolean isConstruct(ItemStack stack) {
        return stack.has(BrightestDayComponents.CONSTRUCT_TOOL);
    }

    public static ItemStack create(ServerPlayer player, ConstructTool tool) {
        int color = CorpsColors.of(player);
        ItemStack stack = new ItemStack(tool.item());
        stack.set(BrightestDayComponents.CONSTRUCT_TOOL, new ConstructToolData(player.getUUID(), player.level().getGameTime() + LIFETIME_TICKS));
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color));
        stack.set(DataComponents.ITEM_NAME, Component.translatable(tool.translationKey()).withColor(color));
        stack.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.DYED_COLOR, true));
        return stack;
    }

    public static void dissolveAll(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (isConstruct(inventory.getItem(slot))) inventory.setItem(slot, ItemStack.EMPTY);
        }
    }

    public static void tick(ServerPlayer player, int serverTick) {
        Inventory inventory = player.getInventory();
        long now = player.level().getGameTime();
        boolean charged = PowerRingItem.hasCharge(player);
        boolean carrying = false;
        boolean dissolved = false;

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            ConstructToolData data = stack.get(BrightestDayComponents.CONSTRUCT_TOOL);
            if (data == null) continue;

            if (!charged || now >= data.expiresAt() || !data.owner().equals(player.getUUID())) {
                inventory.setItem(slot, ItemStack.EMPTY);
                dissolved = true;
            } else {
                carrying = true;
            }
        }

        if (dissolved) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8F, 1.4F);
        }
        if (carrying && serverTick % 20 == 0 && !player.hasInfiniteMaterials()) {
            PowerRingItem.drainWorn(player, DRAIN_PER_SECOND);
        }
    }

    private ConstructTools() {}
}
