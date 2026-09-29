package net.execheinz.upgrader.contract;

import java.util.ArrayList;
import java.util.List;
import net.execheinz.upgrader.cases.CaseDefinition;
import net.execheinz.upgrader.cases.CaseLoot;
import net.execheinz.upgrader.economy.CaseStash;
import net.execheinz.upgrader.network.ClientboundContractResultPacket;
import net.execheinz.upgrader.network.ModNetwork;
import net.execheinz.upgrader.value.ItemValues;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Trade 3–8 items (player inventory or case stash) for one lottery roll.
 */
public final class ContractService {
    public static final int MIN_ITEMS = 3;
    public static final int MAX_ITEMS = 8;
    /** packet.number: 0 = case stash, 1 = player inventory */
    public static final int SOURCE_STASH = 0;
    public static final int SOURCE_PLAYER = 1;

    private ContractService() {
    }

    public static boolean submit(ServerPlayer player, int source, int[] slotIndices) {
        if (slotIndices == null || slotIndices.length < MIN_ITEMS || slotIndices.length > MAX_ITEMS) {
            return false;
        }
        if (source == SOURCE_PLAYER) {
            return submitPlayer(player, slotIndices);
        }
        return submitStash(player, slotIndices);
    }

    private static boolean submitStash(ServerPlayer player, int[] slotIndices) {
        List<ItemStack> stash = CaseStash.get(player);
        ArrayList<Integer> unique = new ArrayList<>();
        long total = 0L;
        Level level = player.getLevel();
        for (int idx : slotIndices) {
            if (idx < 0 || idx >= CaseStash.SLOTS || unique.contains(idx)) {
                return false;
            }
            ItemStack stack = stash.get(idx);
            if (stack.isEmpty()) {
                return false;
            }
            unique.add(idx);
            total += Math.max(0L, Math.round(ItemValues.stackValue(level, stack)));
        }
        if (total <= 0L) {
            return false;
        }
        List<ItemStack> taken = CaseStash.takeSlots(player, unique);
        if (taken.size() != unique.size()) {
            return false;
        }
        return finish(player, total, List.of());
    }

    private static boolean submitPlayer(ServerPlayer player, int[] slotIndices) {
        Inventory inv = player.getInventory();
        ArrayList<Integer> unique = new ArrayList<>();
        long total = 0L;
        Level level = player.getLevel();
        for (int idx : slotIndices) {
            if (idx < 0 || idx >= 36 || unique.contains(idx)) {
                return false;
            }
            ItemStack stack = inv.getItem(idx);
            if (stack.isEmpty()) {
                return false;
            }
            unique.add(idx);
            total += Math.max(0L, Math.round(ItemValues.stackValue(level, stack)));
        }
        if (total <= 0L) {
            return false;
        }
        for (int idx : unique) {
            inv.setItem(idx, ItemStack.EMPTY);
        }
        return finish(player, total, unique);
    }

    private static boolean finish(ServerPlayer player, long total, List<Integer> preferSlots) {
        Level level = player.getLevel();
        Inventory inv = player.getInventory();
        CaseDefinition target = pickCase(total);
        ItemStack reward = CaseLoot.roll(level, target, player.getRandom());
        ItemStack remaining = reward.copy();

        // Put reward into the first emptied slot so it pops in the same grid immediately.
        if (preferSlots != null) {
            for (int idx : preferSlots) {
                if (remaining.isEmpty()) {
                    break;
                }
                if (idx < 0 || idx >= 36) {
                    continue;
                }
                if (!inv.getItem(idx).isEmpty()) {
                    continue;
                }
                int put = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                inv.setItem(idx, remaining.split(put));
            }
        }
        if (!remaining.isEmpty()) {
            CaseStash.deliver(player, remaining);
        }

        // Only sync the slots we touched (cleared + reward placements) — tiny payload.
        int[] changedSlots;
        ItemStack[] changedStacks;
        if (preferSlots == null || preferSlots.isEmpty()) {
            changedSlots = ClientboundContractResultPacket.NO_SLOTS;
            changedStacks = ClientboundContractResultPacket.NO_STACKS;
        } else {
            changedSlots = new int[preferSlots.size()];
            changedStacks = new ItemStack[preferSlots.size()];
            for (int i = 0; i < preferSlots.size(); ++i) {
                int idx = preferSlots.get(i);
                changedSlots[i] = idx;
                changedStacks[i] = inv.getItem(idx).copy();
            }
        }

        long rewardValue = Math.max(0L, Math.round(ItemValues.stackValue(level, reward)));
        ModNetwork.sendTo(player, new ClientboundContractResultPacket(
            true,
            total,
            rewardValue,
            reward.copy(),
            target.id(),
            changedSlots,
            changedStacks
        ));
        CaseStash.sync(player);
        return true;
    }

    private static CaseDefinition pickCase(long totalValue) {
        long aim = Math.max(1L, Math.round(totalValue * 0.90));
        CaseDefinition best = CaseDefinition.all().get(0);
        long bestDist = Long.MAX_VALUE;
        for (CaseDefinition def : CaseDefinition.all()) {
            long dist = Math.abs(def.price() - aim);
            if (dist < bestDist) {
                bestDist = dist;
                best = def;
            }
        }
        return best;
    }
}
