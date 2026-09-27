package net.execheinz.upgrader.menu;

import net.execheinz.upgrader.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class DoubleMenu extends AbstractContainerMenu {
    public DoubleMenu(int windowId, Inventory playerInventory) {
        super(ModMenus.DOUBLE.get(), windowId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}
