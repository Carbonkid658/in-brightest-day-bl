package dev.amble.core.mannequin;

import dev.amble.core.items.LanternBlockItem;
import dev.amble.core.items.PowerRingItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HologramContainer implements Container {
    public static final int RING = 0;
    public static final int LANTERN = 1;
    private static final double REACH = 8.0;

    private final Mannequin mannequin;

    public HologramContainer(Mannequin mannequin) {
        this.mannequin = mannequin;
    }

    private Hologram hologram() {
        return Mannequins.hologram(this.mannequin);
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return this.getItem(RING).isEmpty() && this.getItem(LANTERN).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == LANTERN ? this.mannequin.getItemBySlot(EquipmentSlot.OFFHAND) : this.hologram().ring();
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack stack = this.getItem(slot).copy();
        ItemStack removed = stack.split(count);
        this.setItem(slot, stack);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = this.getItem(slot);
        this.setItem(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        if (slot == LANTERN) this.mannequin.setItemSlot(EquipmentSlot.OFFHAND, itemStack);
        else Mannequins.update(this.mannequin, this.hologram().withRing(itemStack));
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return slot == LANTERN ? itemStack.getItem() instanceof LanternBlockItem : itemStack.getItem() instanceof PowerRingItem;
    }

    @Override
    public void setChanged() {
        Mannequins.update(this.mannequin, this.hologram());
        this.mannequin.setItemSlot(EquipmentSlot.OFFHAND, this.getItem(LANTERN));
    }

    @Override
    public boolean stillValid(Player player) {
        return this.mannequin.isAlive() && player.distanceTo(this.mannequin) <= REACH && Mannequins.mayEdit(this.mannequin, player);
    }

    @Override
    public void clearContent() {
        this.setItem(RING, ItemStack.EMPTY);
        this.setItem(LANTERN, ItemStack.EMPTY);
    }
}
