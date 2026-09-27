package net.execheinz.upgrader.menu;

import net.execheinz.upgrader.economy.CasePending;
import net.execheinz.upgrader.registry.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Cases UI is fully virtual (stash rendered client-side). */
public class CasesMenu extends AbstractContainerMenu {
    public CasesMenu(int windowId, Inventory playerInventory) {
        super(ModMenus.CASES.get(), windowId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer serverPlayer) {
            CasePending.autoKeepIfAny(serverPlayer);
        }
    }
}
