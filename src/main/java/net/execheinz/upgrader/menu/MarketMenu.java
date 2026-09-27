package net.execheinz.upgrader.menu;

import net.execheinz.upgrader.Config;
import net.execheinz.upgrader.registry.ModMenus;
import net.execheinz.upgrader.value.ItemValues;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MarketMenu extends AbstractContainerMenu {
    public static final int SELL_SLOTS = 9;
    public static final int SELL_X = 89;
    public static final int SELL_Y = 92;
    public static final int INV_X = 89;
    public static final int INV_Y = 236;
    public static final int HOTBAR_Y = 294;

    private static final int SELL_START = 0;
    private static final int SELL_END = SELL_SLOTS;
    private static final int INV_START = SELL_END;
    private static final int INV_END = INV_START + 27;
    private static final int HOTBAR_START = INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container sellTray = new SimpleContainer(SELL_SLOTS);
    /** Client-only visual gate so buy tab doesn't show the sell tray slots. */
    public boolean sellTrayVisible = true;

    public MarketMenu(int windowId, Inventory playerInventory) {
        super(ModMenus.MARKET.get(), windowId);
        for (int i = 0; i < SELL_SLOTS; ++i) {
            this.addSlot(new Slot(this.sellTray, i, SELL_X + i * 18, SELL_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !stack.isEmpty() && !ItemValues.isBlacklisted(stack.getItem());
                }

                @Override
                public boolean isActive() {
                    return MarketMenu.this.sellTrayVisible;
                }
            });
        }
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, INV_X + col * 18, HOTBAR_Y));
        }
    }

    public int sellUnitCount() {
        int total = 0;
        for (int i = 0; i < SELL_SLOTS; ++i) {
            ItemStack stack = this.sellTray.getItem(i);
            if (!stack.isEmpty()) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public long sellTotalValue(Level level) {
        long total = 0L;
        for (int i = 0; i < SELL_SLOTS; ++i) {
            ItemStack stack = this.sellTray.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            long raw = Math.round(ItemValues.stackValue(level, stack));
            total += Math.max(0L, Math.round(raw * Config.marketSellRate));
        }
        return total;
    }

    public boolean hasSellableItems() {
        for (int i = 0; i < SELL_SLOTS; ++i) {
            ItemStack stack = this.sellTray.getItem(i);
            if (!stack.isEmpty() && !ItemValues.isBlacklisted(stack.getItem())) {
                return true;
            }
        }
        return false;
    }

    /** Clears tray after a successful sale (items already valued/consumed). */
    public void clearSellTray() {
        this.sellTray.clearContent();
        this.broadcastChanges();
    }

    public Container sellTray() {
        return this.sellTray;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, this.sellTray);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < SELL_END) {
            if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (ItemValues.isBlacklisted(stack.getItem()) || !this.moveItemStackTo(stack, SELL_START, SELL_END, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}
