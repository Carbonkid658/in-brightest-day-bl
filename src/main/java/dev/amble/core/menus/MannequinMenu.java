package dev.amble.core.menus;

import dev.amble.core.BrightestDayMenus;
import dev.amble.core.items.LanternBlockItem;
import dev.amble.core.items.PowerRingItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MannequinMenu extends AbstractContainerMenu {
    public static final int RING_SLOT_X = 12;
    public static final int RING_SLOT_Y = 40;
    public static final int LANTERN_SLOT_X = 12;
    public static final int LANTERN_SLOT_Y = 72;
    public static final int INVENTORY_X = 61;
    public static final int INVENTORY_Y = 192;

    private final Container ring;
    private final int entityId;

    public MannequinMenu(int containerId, Inventory inventory, Integer entityId) {
        this(containerId, inventory, entityId, new SimpleContainer(2));
    }

    public MannequinMenu(int containerId, Inventory inventory, int entityId, Container ring) {
        super(BrightestDayMenus.MANNEQUIN, containerId);
        this.ring = ring;
        this.entityId = entityId;

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

        this.addSlot(new Slot(ring, 1, LANTERN_SLOT_X, LANTERN_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.getItem() instanceof LanternBlockItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        this.addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
    }

    public int entityId() {
        return this.entityId;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (slotIndex < 2) {
            if (!this.moveItemStackTo(stack, 2, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof PowerRingItem) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (!(stack.getItem() instanceof LanternBlockItem) || !this.moveItemStackTo(stack, 1, 2, false)) {
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
