package net.execheinz.upgrader.cases;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.execheinz.upgrader.value.ItemValues;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Two-step roll: first pick rarity with real CS-like odds, then pick an item inside that tier.
 * Gold (LEGENDARY) and purple (UNCOMMON) actually drop; house edge comes from junk/common filling the rest.
 */
public final class CaseLoot {
    /** Absolute tier odds — sum = 1.0 */
    private static final Map<CaseDefinition.CaseTier, Double> TIER_CHANCE = new EnumMap<>(CaseDefinition.CaseTier.class);

    static {
        TIER_CHANCE.put(CaseDefinition.CaseTier.JUNK, 0.52);
        TIER_CHANCE.put(CaseDefinition.CaseTier.COMMON, 0.28);
        TIER_CHANCE.put(CaseDefinition.CaseTier.UNCOMMON, 0.14); // фиолетовый
        TIER_CHANCE.put(CaseDefinition.CaseTier.RARE, 0.045);    // розовый
        TIER_CHANCE.put(CaseDefinition.CaseTier.LEGENDARY, 0.015); // золотой
    }

    private CaseLoot() {
    }

    public static double chancePercent(Level level, CaseDefinition def, CaseDefinition.CaseEntry entry) {
        List<CaseDefinition.CaseEntry> sameTier = entriesOf(def, entry.tier());
        if (sameTier.isEmpty()) {
            return 0.0;
        }
        double tierShare = TIER_CHANCE.getOrDefault(entry.tier(), 0.0) * 100.0;
        double[] w = itemWeights(level, sameTier);
        double total = 0.0;
        double mine = 0.0;
        for (int i = 0; i < sameTier.size(); ++i) {
            total += w[i];
            CaseDefinition.CaseEntry e = sameTier.get(i);
            if (e == entry || (e.item() == entry.item() && e.count() == entry.count() && e.tier() == entry.tier())) {
                mine = w[i];
            }
        }
        return total <= 0.0 ? 0.0 : tierShare * mine / total;
    }

    public static double expectedValue(Level level, CaseDefinition def) {
        double ev = 0.0;
        for (CaseDefinition.CaseTier tier : CaseDefinition.CaseTier.values()) {
            List<CaseDefinition.CaseEntry> entries = entriesOf(def, tier);
            if (entries.isEmpty()) {
                continue;
            }
            double[] w = itemWeights(level, entries);
            double totalW = 0.0;
            double tierEv = 0.0;
            for (int i = 0; i < entries.size(); ++i) {
                CaseDefinition.CaseEntry e = entries.get(i);
                double value = Math.max(0.0, ItemValues.unitValue(level, e.item()) * e.count());
                totalW += w[i];
                tierEv += w[i] * value;
            }
            if (totalW > 0.0) {
                ev += TIER_CHANCE.getOrDefault(tier, 0.0) * (tierEv / totalW);
            }
        }
        return ev;
    }

    public static ItemStack roll(Level level, CaseDefinition def, RandomSource random) {
        CaseDefinition.CaseTier tier = rollTier(def, random);
        List<CaseDefinition.CaseEntry> entries = entriesOf(def, tier);
        if (entries.isEmpty()) {
            entries = def.pool();
        }
        CaseDefinition.CaseEntry pick = pickInside(level, entries, random);
        return new ItemStack(pick.item(), pick.count());
    }

    public static List<ItemStack> rollMany(Level level, CaseDefinition def, RandomSource random, int count) {
        ArrayList<ItemStack> out = new ArrayList<>(count);
        for (int i = 0; i < count; ++i) {
            out.add(roll(level, def, random));
        }
        return out;
    }

    /** Inside a tier — expensive stacks drop much rarer. */
    private static double[] itemWeights(Level level, List<CaseDefinition.CaseEntry> entries) {
        double[] w = new double[entries.size()];
        for (int i = 0; i < entries.size(); ++i) {
            CaseDefinition.CaseEntry e = entries.get(i);
            double value = Math.max(1.0, ItemValues.unitValue(level, e.item()) * e.count());
            w[i] = 1.0 / Math.pow(value, 0.72);
        }
        return w;
    }

    private static CaseDefinition.CaseTier rollTier(CaseDefinition def, RandomSource random) {
        double available = 0.0;
        for (CaseDefinition.CaseTier tier : CaseDefinition.CaseTier.values()) {
            if (!entriesOf(def, tier).isEmpty()) {
                available += TIER_CHANCE.getOrDefault(tier, 0.0);
            }
        }
        if (available <= 0.0) {
            return CaseDefinition.CaseTier.JUNK;
        }
        double roll = random.nextDouble() * available;
        double cursor = 0.0;
        CaseDefinition.CaseTier last = CaseDefinition.CaseTier.JUNK;
        for (CaseDefinition.CaseTier tier : CaseDefinition.CaseTier.values()) {
            if (entriesOf(def, tier).isEmpty()) {
                continue;
            }
            last = tier;
            cursor += TIER_CHANCE.getOrDefault(tier, 0.0);
            if (roll <= cursor) {
                return tier;
            }
        }
        return last;
    }

    private static CaseDefinition.CaseEntry pickInside(Level level, List<CaseDefinition.CaseEntry> entries, RandomSource random) {
        double[] w = itemWeights(level, entries);
        double total = 0.0;
        for (double v : w) {
            total += v;
        }
        double roll = random.nextDouble() * total;
        double cursor = 0.0;
        for (int i = 0; i < entries.size(); ++i) {
            cursor += w[i];
            if (roll <= cursor) {
                return entries.get(i);
            }
        }
        return entries.get(entries.size() - 1);
    }

    private static List<CaseDefinition.CaseEntry> entriesOf(CaseDefinition def, CaseDefinition.CaseTier tier) {
        ArrayList<CaseDefinition.CaseEntry> out = new ArrayList<>();
        for (CaseDefinition.CaseEntry e : def.pool()) {
            if (e.tier() == tier) {
                out.add(e);
            }
        }
        return out;
    }
}
