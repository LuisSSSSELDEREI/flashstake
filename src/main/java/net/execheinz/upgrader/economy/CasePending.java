package net.execheinz.upgrader.economy;

import java.util.ArrayList;
import java.util.List;
import net.execheinz.upgrader.network.ClientboundCasePendingPacket;
import net.execheinz.upgrader.network.ModNetwork;
import net.execheinz.upgrader.value.ItemValues;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Loot waiting for Keep / Sell after opening a case. */
public final class CasePending {
    private static final String KEY = "FlashStakeCasePending";

    private CasePending() {
    }

    public static List<ItemStack> get(ServerPlayer player) {
        ArrayList<ItemStack> out = new ArrayList<>();
        CompoundTag data = player.getPersistentData();
        if (!data.contains(KEY, Tag.TAG_LIST)) {
            return out;
        }
        ListTag list = data.getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); ++i) {
            ItemStack stack = ItemStack.of(list.getCompound(i));
            if (!stack.isEmpty()) {
                out.add(stack);
            }
        }
        return out;
    }

    public static void set(ServerPlayer player, List<ItemStack> stacks) {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag tag = new CompoundTag();
            stack.copy().save(tag);
            list.add(tag);
        }
        if (list.isEmpty()) {
            player.getPersistentData().remove(KEY);
        } else {
            player.getPersistentData().put(KEY, list);
        }
        sync(player);
    }

    public static void clear(ServerPlayer player) {
        player.getPersistentData().remove(KEY);
        sync(player);
    }

    public static boolean isEmpty(ServerPlayer player) {
        return get(player).isEmpty();
    }

    public static void keep(ServerPlayer player) {
        List<ItemStack> stacks = get(player);
        if (stacks.isEmpty()) {
            return;
        }
        clear(player);
        CaseStash.addAll(player, stacks);
    }

    public static long sell(ServerPlayer player) {
        List<ItemStack> stacks = get(player);
        if (stacks.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (ItemStack stack : stacks) {
            total += Math.max(0L, Math.round(ItemValues.stackValue(player.level(), stack)));
        }
        clear(player);
        if (total > 0L) {
            PlayerBalance.add(player, total);
        }
        return total;
    }

    /** If player leaves without choosing, auto-stash so loot is not lost. */
    public static void autoKeepIfAny(ServerPlayer player) {
        if (!isEmpty(player)) {
            keep(player);
        }
    }

    public static void sync(ServerPlayer player) {
        ModNetwork.sendTo(player, new ClientboundCasePendingPacket(get(player)));
    }
}
