package net.execheinz.upgrader.economy;

import java.util.ArrayList;
import java.util.List;
import net.execheinz.upgrader.network.ClientboundCaseStashPacket;
import net.execheinz.upgrader.network.ModNetwork;
import net.execheinz.upgrader.value.ItemValues;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CaseStash {
    public static final int SLOTS = 27;
    private static final String KEY = "FlashStakeCaseStash";

    private CaseStash() {
    }

    public static List<ItemStack> get(Player player) {
        ArrayList<ItemStack> out = new ArrayList<>(SLOTS);
        CompoundTag data = player.getPersistentData();
        if (!data.contains(KEY, Tag.TAG_LIST)) {
            for (int i = 0; i < SLOTS; ++i) {
                out.add(ItemStack.EMPTY);
            }
            return out;
        }
        ListTag list = data.getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < SLOTS; ++i) {
            if (i < list.size()) {
                out.add(ItemStack.of(list.getCompound(i)));
            } else {
                out.add(ItemStack.EMPTY);
            }
        }
        return out;
    }

    public static void set(ServerPlayer player, List<ItemStack> stacks) {
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; ++i) {
            ItemStack stack = i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;
            CompoundTag tag = new CompoundTag();
            if (!stack.isEmpty()) {
                stack.copy().save(tag);
            }
            list.add(tag);
        }
        player.getPersistentData().put(KEY, list);
        sync(player);
    }

    public static boolean add(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        List<ItemStack> slots = get(player);
        ItemStack remaining = stack.copy();
        for (int i = 0; i < SLOTS && !remaining.isEmpty(); ++i) {
            ItemStack slot = slots.get(i);
            if (slot.isEmpty()) {
                continue;
            }
            if (!ItemStack.isSameItemSameTags(slot, remaining)) {
                continue;
            }
            int space = Math.min(slot.getMaxStackSize() - slot.getCount(), remaining.getCount());
            if (space <= 0) {
                continue;
            }
            slot.grow(space);
            remaining.shrink(space);
        }
        for (int i = 0; i < SLOTS && !remaining.isEmpty(); ++i) {
            if (!slots.get(i).isEmpty()) {
                continue;
            }
            int put = Math.min(remaining.getMaxStackSize(), remaining.getCount());
            slots.set(i, remaining.split(put));
        }
        set(player, slots);
        if (!remaining.isEmpty()) {
            // overflow → player inv / drop
            if (!player.getInventory().add(remaining.copy())) {
                player.drop(remaining.copy(), false);
            }
            return false;
        }
        return true;
    }

    public static void addAll(ServerPlayer player, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            add(player, stack);
        }
    }

    public static boolean withdraw(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= SLOTS) {
            return false;
        }
        List<ItemStack> slots = get(player);
        ItemStack stack = slots.get(slot);
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack copy = stack.copy();
        slots.set(slot, ItemStack.EMPTY);
        set(player, slots);
        if (!player.getInventory().add(copy)) {
            player.drop(copy, false);
        }
        return true;
    }

    public static int withdrawAll(ServerPlayer player) {
        List<ItemStack> slots = get(player);
        int moved = 0;
        for (int i = 0; i < SLOTS; ++i) {
            ItemStack stack = slots.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack copy = stack.copy();
            slots.set(i, ItemStack.EMPTY);
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
            ++moved;
        }
        set(player, slots);
        return moved;
    }

    public static long sell(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= SLOTS) {
            return 0L;
        }
        List<ItemStack> slots = get(player);
        ItemStack stack = slots.get(slot);
        if (stack.isEmpty()) {
            return 0L;
        }
        long value = Math.max(0L, Math.round(ItemValues.stackValue(player.getLevel(), stack)));
        slots.set(slot, ItemStack.EMPTY);
        set(player, slots);
        if (value > 0L) {
            PlayerBalance.add(player, value);
        }
        return value;
    }

    public static long sellAll(ServerPlayer player) {
        Level level = player.getLevel();
        List<ItemStack> slots = get(player);
        long total = 0L;
        for (int i = 0; i < SLOTS; ++i) {
            ItemStack stack = slots.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            total += Math.max(0L, Math.round(ItemValues.stackValue(level, stack)));
            slots.set(i, ItemStack.EMPTY);
        }
        set(player, slots);
        if (total > 0L) {
            PlayerBalance.add(player, total);
        }
        return total;
    }

    public static long totalValue(Player player) {
        long total = 0L;
        Level level = player.getLevel();
        for (ItemStack stack : get(player)) {
            if (!stack.isEmpty()) {
                total += Math.max(0L, Math.round(ItemValues.stackValue(level, stack)));
            }
        }
        return total;
    }

    public static void sync(ServerPlayer player) {
        ModNetwork.sendTo(player, new ClientboundCaseStashPacket(get(player)));
    }
}
