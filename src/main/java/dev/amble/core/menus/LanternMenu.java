package dev.amble.core.menus;

import dev.amble.core.BrightestDayMenus;
import dev.amble.core.items.PowerRingItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class LanternMenu extends AbstractContainerMenu {
    public static final int RING_SLOT_X = 26;
    public static final int RING_SLOT_Y = 26;
    public static final int INVENTORY_Y = 124;

    private final Container ring;

    public LanternMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(1));
    }

    public LanternMenu(int containerId, Inventory inventory, Container ring) {
        super(BrightestDayMenus.LANTERN, containerId);
        this.ring = ring;

        this.addSlot(new Slot(ring, 0, RING_SLOT_X, RING_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.getItem() instanceof PowerRingItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        this.addStandardInventorySlots(inventory, 8, INVENTORY_Y);
    }

    public ItemStack getRing() {
        return this.ring.getItem(0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (slotIndex == 0) {
            if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (!(stack.getItem() instanceof PowerRingItem) || !this.moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return clicked;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.ring.stillValid(player);
    }
}
