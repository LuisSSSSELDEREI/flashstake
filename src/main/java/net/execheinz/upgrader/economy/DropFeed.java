package net.execheinz.upgrader.economy;

import java.util.ArrayList;
import java.util.List;
import net.execheinz.upgrader.cases.CaseDefinition;
import net.execheinz.upgrader.network.ClientboundDropFeedPacket;
import net.execheinz.upgrader.network.ModNetwork;
import net.execheinz.upgrader.value.ItemValues;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Broadcasts notable case drops to every online player. */
public final class DropFeed {
    private static final long MIN_VALUE = 400L;

    private DropFeed() {
    }

    public static void maybeBroadcast(ServerPlayer opener, String caseId, List<ItemStack> rewards) {
        CaseDefinition def = CaseDefinition.byId(caseId);
        ArrayList<ClientboundDropFeedPacket.Entry> entries = new ArrayList<>();
        for (ItemStack stack : rewards) {
            if (stack.isEmpty()) {
                continue;
            }
            CaseDefinition.CaseTier tier = tierOf(def, stack);
            long value = Math.round(ItemValues.stackValue(opener.level(), stack));
            boolean notable = tier == CaseDefinition.CaseTier.RARE
                || tier == CaseDefinition.CaseTier.LEGENDARY
                || value >= MIN_VALUE;
            if (!notable) {
                continue;
            }
            entries.add(new ClientboundDropFeedPacket.Entry(
                opener.getGameProfile().getName(),
                caseId,
                stack.copy(),
                value,
                tier.ordinal()
            ));
        }
        if (!entries.isEmpty()) {
            ModNetwork.sendToAll(new ClientboundDropFeedPacket(entries));
        }
    }

    private static CaseDefinition.CaseTier tierOf(CaseDefinition def, ItemStack stack) {
        if (def == null) {
            return CaseDefinition.CaseTier.COMMON;
        }
        CaseDefinition.CaseTier best = CaseDefinition.CaseTier.JUNK;
        for (CaseDefinition.CaseEntry e : def.pool()) {
            if (e.item() == stack.getItem()) {
                if (e.count() == stack.getCount()) {
                    return e.tier();
                }
                if (e.tier().ordinal() > best.ordinal()) {
                    best = e.tier();
                }
            }
        }
        return best;
    }
}
