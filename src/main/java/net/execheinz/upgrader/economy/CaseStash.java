package net.execheinz.upgrader.economy;

import java.util.ArrayList;
import java.util.List;
import net.execheinz.upgrader.network.ClientboundCaseStashPacket;
import net.execheinz.upgrader.network.ModNetwork;
import net.execheinz.upgrader.value.ItemValues;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Case profile inventory — 54 slots, stacks up to 999 so loot stays visible.
 */
public final class CaseStash {
    public static final int SLOTS = 54;
    public static final int MAX_STACK = 999;
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
        HolderLookup.Provider lookup = player.registryAccess();
        int n = Math.min(list.size(), SLOTS);
        for (int i = 0; i < n; ++i) {
            ItemStack stack = ItemStack.parseOptional(lookup, list.getCompound(i));
            if (!stack.isEmpty() && stack.getCount() > MAX_STACK) {
                stack.setCount(MAX_STACK);
            }
            out.add(stack);
        }
        while (out.size() < SLOTS) {
            out.add(ItemStack.EMPTY);
        }
        return out;
    }

    public static void set(ServerPlayer player, List<ItemStack> stacks) {
        ListTag list = new ListTag();
        HolderLookup.Provider lookup = player.registryAccess();
        for (int i = 0; i < SLOTS; ++i) {
            ItemStack stack = i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;
            if (stack.isEmpty()) {
                list.add(new CompoundTag());
            } else {
                ItemStack copy = stack.copy();
                if (copy.getCount() > MAX_STACK) {
                    copy.setCount(MAX_STACK);
                }
                Tag saved = copy.save(lookup);
                list.add(saved instanceof CompoundTag tag ? tag : new CompoundTag());
            }
        }
        player.getPersistentData().put(KEY, list);
        sync(player);
    }

    /** Compact same-item stacks up to MAX_STACK (keeps items visible / frees slots). */
    public static void compact(ServerPlayer player) {
        List<ItemStack> slots = get(player);
        ArrayList<ItemStack> merged = new ArrayList<>();
        for (ItemStack raw : slots) {
            if (raw.isEmpty()) {
                continue;
            }
            ItemStack stack = raw.copy();
            for (ItemStack into : merged) {
                if (stack.isEmpty()) {
                    break;
                }
                if (!ItemStack.isSameItemSameComponents(into, stack)) {
                    continue;
                }
                int space = MAX_STACK - into.getCount();
                if (space <= 0) {
                    continue;
                }
                int move = Math.min(space, stack.getCount());
                into.grow(move);
                stack.shrink(move);
            }
            while (!stack.isEmpty()) {
                int put = Math.min(MAX_STACK, stack.getCount());
                ItemStack part = stack.copy();
                part.setCount(put);
                stack.shrink(put);
                merged.add(part);
            }
        }
        ArrayList<ItemStack> out = new ArrayList<>(SLOTS);
        for (int i = 0; i < SLOTS; ++i) {
            out.add(i < merged.size() ? merged.get(i) : ItemStack.EMPTY);
        }
        for (int i = SLOTS; i < merged.size(); ++i) {
            giveToPlayer(player, merged.get(i));
        }
        set(player, out);
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
            if (!ItemStack.isSameItemSameComponents(slot, remaining)) {
                continue;
            }
            int space = Math.min(MAX_STACK - slot.getCount(), remaining.getCount());
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
            int put = Math.min(MAX_STACK, remaining.getCount());
            slots.set(i, remaining.split(put));
        }
        set(player, slots);
        if (!remaining.isEmpty()) {
            giveToPlayer(player, remaining);
            return false;
        }
        return true;
    }

    /** Put item straight into player inventory (overflow drops on ground). */
    public static void deliver(ServerPlayer player, ItemStack stack) {
        giveToPlayer(player, stack);
    }

    private static void giveToPlayer(ServerPlayer player, ItemStack stack) {
        ItemStack left = stack.copy();
        while (!left.isEmpty()) {
            int chunk = Math.min(left.getMaxStackSize(), left.getCount());
            ItemStack piece = left.split(chunk);
            if (!player.getInventory().add(piece.copy())) {
                player.drop(piece.copy(), false);
            }
        }
    }

    public static void addAll(ServerPlayer player, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            add(player, stack);
        }
        compact(player);
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
        giveToPlayer(player, copy);
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
            giveToPlayer(player, copy);
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
        long value = Math.max(0L, Math.round(ItemValues.stackValue(player.level(), stack)));
        slots.set(slot, ItemStack.EMPTY);
        set(player, slots);
        if (value > 0L) {
            PlayerBalance.add(player, value);
        }
        return value;
    }

    public static List<ItemStack> takeSlots(ServerPlayer player, List<Integer> indices) {
        if (indices == null || indices.isEmpty()) {
            return List.of();
        }
        List<ItemStack> slots = get(player);
        ArrayList<ItemStack> taken = new ArrayList<>(indices.size());
        for (int idx : indices) {
            if (idx < 0 || idx >= SLOTS) {
                return List.of();
            }
            ItemStack stack = slots.get(idx);
            if (stack.isEmpty()) {
                return List.of();
            }
            taken.add(stack.copy());
        }
        for (int idx : indices) {
            slots.set(idx, ItemStack.EMPTY);
        }
        set(player, slots);
        return taken;
    }

    public static long sellAll(ServerPlayer player) {
        Level level = player.level();
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
        Level level = player.level();
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
