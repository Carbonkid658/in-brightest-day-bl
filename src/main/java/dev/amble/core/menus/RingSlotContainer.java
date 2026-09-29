package dev.amble.core.menus;

import dev.amble.core.BrightestDayAttachments;
import dev.amble.core.items.PowerRingItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class RingSlotContainer implements Container {
    private final Player player;

    public RingSlotContainer(Player player) {
        this.player = player;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return BrightestDayAttachments.getRing(this.player).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return BrightestDayAttachments.getRing(this.player);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack ring = BrightestDayAttachments.getRing(this.player).copy();
        ItemStack removed = ring.split(count);
        BrightestDayAttachments.setRing(this.player, ring);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack ring = BrightestDayAttachments.getRing(this.player);
        BrightestDayAttachments.setRing(this.player, ItemStack.EMPTY);
        return ring;
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        BrightestDayAttachments.setRing(this.player, itemStack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return itemStack.getItem() instanceof PowerRingItem;
    }

    @Override
    public void setChanged() {
        BrightestDayAttachments.setRing(this.player, BrightestDayAttachments.getRing(this.player));
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player;
    }

    @Override
    public void clearContent() {
        BrightestDayAttachments.setRing(this.player, ItemStack.EMPTY);
    }
}
