package net.execheinz.upgrader.client;

import java.util.ArrayList;
import java.util.List;
import net.execheinz.upgrader.cases.CaseDefinition;
import net.execheinz.upgrader.network.ClientboundDropFeedPacket;
import net.minecraft.world.item.ItemStack;

/** Client-side ring buffer of notable case drops. */
public final class DropFeedClient {
    public static final int MAX = 12;

    public record Entry(String playerName, String caseId, ItemStack stack, long value, CaseDefinition.CaseTier tier, long atMs) {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private DropFeedClient() {
    }

    public static synchronized void push(ClientboundDropFeedPacket packet) {
        long now = System.currentTimeMillis();
        for (ClientboundDropFeedPacket.Entry e : packet.entries()) {
            CaseDefinition.CaseTier tier = CaseDefinition.CaseTier.values()[
                Math.max(0, Math.min(e.tierOrdinal(), CaseDefinition.CaseTier.values().length - 1))];
            ENTRIES.add(0, new Entry(e.playerName(), e.caseId(), e.stack().copy(), e.value(), tier, now));
        }
        while (ENTRIES.size() > MAX) {
            ENTRIES.remove(ENTRIES.size() - 1);
        }
    }

    public static synchronized List<Entry> snapshot() {
        return List.copyOf(ENTRIES);
    }
}
