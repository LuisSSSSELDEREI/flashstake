/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.Rarity
 *  net.minecraft.world.item.SpawnEggItem
 *  net.minecraft.world.item.crafting.Ingredient
 *  net.minecraft.world.item.crafting.Recipe
 *  net.minecraft.world.item.crafting.SmithingRecipe
 *  net.minecraft.world.item.crafting.SmithingTrimRecipe
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraftforge.registries.ForgeRegistries
 */
package net.execheinz.upgrader.value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.execheinz.upgrader.Config;
import net.execheinz.upgrader.value.BaseValues;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public final class ItemValues {
    private static final int MAX_PASSES = 16;
    /** Hard floor — no item may be worth less than 1. */
    private static final double MIN_UNIT = 1.0;
    private static volatile Map<Item, Double> values;

    public static void invalidate() {
        values = null;
    }

    public static boolean isBlacklisted(Item item) {
        if (item == Items.AIR || BaseValues.isBlacklisted(item)) {
            return true;
        }
        if (item instanceof SpawnEggItem) {
            return true;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key != null && Config.blacklist.contains(key.toString());
    }

    public static double unitValue(Level level, Item item) {
        Double value = ItemValues.table(level).get(item);
        if (value != null) {
            return Math.max(MIN_UNIT, value);
        }
        Double cheap = ItemValues.cheapTagValue(item);
        double raw = cheap != null ? cheap : ItemValues.rarityFallback(item);
        return Math.max(MIN_UNIT, raw);
    }

    public static double stackValue(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0;
        }
        double value = ItemValues.unitValue(level, stack.getItem()) * (double)stack.getCount();
        if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
            double remaining = 1.0 - (double)stack.getDamageValue() / (double)stack.getMaxDamage();
            value *= Math.max(0.05, remaining);
        }
        return value;
    }

    public static List<Item> catalog(Level level) {
        Map<Item, Double> table = ItemValues.table(level);
        ArrayList<Item> items = new ArrayList<Item>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (ItemValues.isBlacklisted(item)) continue;
            items.add(item);
        }
        items.sort((a, b) -> {
            int byValue = Double.compare(table.getOrDefault(a, ItemValues.rarityFallback(a)), table.getOrDefault(b, ItemValues.rarityFallback(b)));
            if (byValue != 0) {
                return byValue;
            }
            return String.valueOf(ForgeRegistries.ITEMS.getKey(a)).compareTo(String.valueOf(ForgeRegistries.ITEMS.getKey(b)));
        });
        return items;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private static Map<Item, Double> table(Level level) {
        Map<Item, Double> local = values;
        if (local != null) return local;
        Class<ItemValues> clazz = ItemValues.class;
        synchronized (ItemValues.class) {
            local = values;
            if (local != null) return local;
            values = local = ItemValues.compute(level);
            // ** MonitorExit[var2_2] (shouldn't be in output)
            return local;
        }
    }

    private static Map<Item, Double> compute(Level level) {
        HashMap<Item, Double> table = new HashMap<Item, Double>();
        HashSet<Item> pinned = new HashSet<Item>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            Double value = ItemValues.configOverride(item);
            if (value == null) {
                value = BaseValues.get(item);
            }
            if (value == null) continue;
            table.put(item, Math.max(MIN_UNIT, value));
            pinned.add(item);
        }
        List<PricedRecipe> recipes = ItemValues.normalize(level);
        for (int pass = 0; pass < 16; ++pass) {
            boolean changed = false;
            for (PricedRecipe recipe : recipes) {
                double cost;
                if (pinned.contains(recipe.result()) || Double.isInfinite(cost = recipe.cost(table))) continue;
                cost = Math.max(MIN_UNIT, cost);
                Double current = (Double)table.get(recipe.result());
                if (current != null && !(cost < current - 1.0E-6)) continue;
                table.put(recipe.result(), cost);
                changed = true;
            }
            if (!changed) break;
        }
        // Ores / raw smeltables: price equals the cooking result (diamond ore == diamond, etc.)
        ItemValues.applyCookingInputs(level, table, pinned);
        // Tag-based fillers (any leaves/flowers not already priced)
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (pinned.contains(item) || table.containsKey(item)) {
                continue;
            }
            Double cheap = ItemValues.cheapTagValue(item);
            if (cheap != null) {
                table.put(item, Math.max(MIN_UNIT, cheap));
            }
        }
        // Seed rarity/tag prices into table so clamp sees fragment→disc etc.
        ItemValues.seedUnsetRecipeItems(recipes, table);
        // Kill craft→sell arbitrage (output cannot be worth more than fair craft cost)
        ItemValues.clampCraftArbitrage(recipes, table, pinned);
        // Slabs always < full block; stairs = block + 1
        ItemValues.enforceBlockFamilyPricing(recipes, table, pinned);
        // Final floor — nothing below 1
        for (Map.Entry<Item, Double> e : table.entrySet()) {
            if (e.getValue() < MIN_UNIT) {
                e.setValue(MIN_UNIT);
            }
        }
        return table;
    }

    /**
     * After all craft pricing:
     * - slabs = half of source block (never ≥ block)
     * - stairs = block + 1
     */
    private static void enforceBlockFamilyPricing(List<PricedRecipe> recipes, Map<Item, Double> table, Set<Item> pinned) {
        for (int pass = 0; pass < 8; ++pass) {
            boolean changed = false;
            for (PricedRecipe recipe : recipes) {
                Item result = recipe.result();
                Item block = ItemValues.cheapestInput(recipe, table);
                if (block == null || block == result) {
                    continue;
                }
                Double blockVal = table.get(block);
                if (blockVal == null) {
                    continue;
                }
                if (result.builtInRegistryHolder().is(ItemTags.SLABS)) {
                    double want = Math.max(MIN_UNIT, blockVal / 2.0);
                    if (want + 1.0E-6 >= blockVal) {
                        // Block too low for a half-split — lift block so slab can be half
                        want = Math.max(MIN_UNIT, blockVal);
                        if (!pinned.contains(block)) {
                            table.put(block, Math.max(blockVal, 2.0 * MIN_UNIT));
                            blockVal = table.get(block);
                            want = Math.max(MIN_UNIT, blockVal / 2.0);
                            changed = true;
                        } else {
                            want = Math.max(MIN_UNIT, blockVal - 1.0);
                        }
                    }
                    Double cur = table.get(result);
                    if (cur == null || Math.abs(cur - want) > 1.0E-6) {
                        table.put(result, want);
                        changed = true;
                    }
                } else if (result.builtInRegistryHolder().is(ItemTags.STAIRS)) {
                    double want = blockVal + 1.0;
                    Double cur = table.get(result);
                    if (cur == null || Math.abs(cur - want) > 1.0E-6) {
                        table.put(result, want);
                        changed = true;
                    }
                }
            }
            if (!changed) {
                break;
            }
        }
    }

    @Nullable
    private static Item cheapestInput(PricedRecipe recipe, Map<Item, Double> table) {
        Item best = null;
        double bestVal = Double.POSITIVE_INFINITY;
        for (List<Item> group : recipe.ingredientGroups()) {
            for (Item item : group) {
                Double v = table.get(item);
                if (v != null && v < bestVal) {
                    bestVal = v;
                    best = item;
                }
            }
        }
        return best;
    }

    /** Put rarity/tag fallbacks into the table for any recipe item still missing — clamp is blind without this. */
    private static void seedUnsetRecipeItems(List<PricedRecipe> recipes, Map<Item, Double> table) {
        for (PricedRecipe recipe : recipes) {
            for (List<Item> group : recipe.ingredientGroups()) {
                for (Item item : group) {
                    if (!table.containsKey(item) && !ItemValues.isBlacklisted(item)) {
                        table.put(item, ItemValues.fallbackUnit(item));
                    }
                }
            }
            Item result = recipe.result();
            if (!table.containsKey(result) && !ItemValues.isBlacklisted(result)) {
                table.put(result, ItemValues.fallbackUnit(result));
            }
        }
    }

    private static double fallbackUnit(Item item) {
        Double cheap = ItemValues.cheapTagValue(item);
        return Math.max(MIN_UNIT, cheap != null ? cheap : ItemValues.rarityFallback(item));
    }

    private static void clampCraftArbitrage(List<PricedRecipe> recipes, Map<Item, Double> table, Set<Item> pinned) {
        for (int pass = 0; pass < 16; ++pass) {
            boolean changed = false;
            for (PricedRecipe recipe : recipes) {
                if (ItemValues.isCompressionRecipe(recipe)) {
                    // Ignore slab/stair packing back into blocks — those ratios fight cutting recipes
                    continue;
                }
                double fair = recipe.cost(table);
                if (Double.isInfinite(fair)) {
                    continue;
                }
                fair = Math.max(MIN_UNIT, fair);
                Double current = table.get(recipe.result());
                if (current == null) {
                    table.put(recipe.result(), fair);
                    changed = true;
                    continue;
                }
                // Cutting 1 block → N slabs: if slabs would print, raise the block instead of crushing slabs below floor
                if (ItemValues.isSlabCuttingRecipe(recipe) && current > fair + 1.0E-6) {
                    if (ItemValues.bumpCuttingInputs(recipe, table, current * recipe.count())) {
                        changed = true;
                    }
                    // Recompute after bump; still clamp output if somehow still high
                    fair = Math.max(MIN_UNIT, recipe.cost(table));
                }
                // Output must not be worth more than crafting it
                if (current > fair + 1.0E-6) {
                    if (pinned.contains(recipe.result())) {
                        // Dupe recipes (1×result in → 2×result out): bump other inputs if possible
                        if (ItemValues.isResultDupeRecipe(recipe) && ItemValues.bumpNonResultInputs(recipe, table, current * recipe.count())) {
                            changed = true;
                        }
                        continue;
                    }
                    table.put(recipe.result(), Math.max(MIN_UNIT, fair));
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
    }

    /** Smithing/crafting that consumes the result item and yields more of it (template duplication). */
    private static boolean isResultDupeRecipe(PricedRecipe recipe) {
        if (recipe.count() < 2) {
            return false;
        }
        for (List<Item> group : recipe.ingredientGroups()) {
            if (group.contains(recipe.result())) {
                return true;
            }
        }
        return false;
    }

    private static boolean bumpNonResultInputs(PricedRecipe recipe, Map<Item, Double> table, double neededTotal) {
        ArrayList<Item> others = new ArrayList<>();
        double resultCost = 0.0;
        for (List<Item> group : recipe.ingredientGroups()) {
            Item best = null;
            double bestVal = Double.POSITIVE_INFINITY;
            for (Item item : group) {
                Double v = table.get(item);
                if (v != null && v < bestVal) {
                    bestVal = v;
                    best = item;
                }
            }
            if (best == null) {
                continue;
            }
            if (best == recipe.result()) {
                resultCost += bestVal;
            } else {
                others.add(best);
            }
        }
        if (others.isEmpty()) {
            return false;
        }
        double needOthers = Math.max(0.0, neededTotal - resultCost);
        double per = needOthers / (double) others.size();
        boolean changed = false;
        for (Item item : others) {
            Double cur = table.get(item);
            if (cur == null || cur + 1.0E-6 < per) {
                table.put(item, Math.max(MIN_UNIT, per));
                changed = true;
            }
        }
        return changed;
    }

    /** 1 (or few) full blocks → several slabs. */
    private static boolean isSlabCuttingRecipe(PricedRecipe recipe) {
        if (recipe.count() < 2) {
            return false;
        }
        return recipe.result().builtInRegistryHolder().is(ItemTags.SLABS);
    }

    /** Raise each ingredient so total craft cost ≥ needed (stops block→slabs print with MIN_UNIT slabs). */
    private static boolean bumpCuttingInputs(PricedRecipe recipe, Map<Item, Double> table, double neededTotal) {
        List<Item> inputs = new ArrayList<>();
        for (List<Item> group : recipe.ingredientGroups()) {
            Item best = null;
            double bestVal = Double.POSITIVE_INFINITY;
            for (Item item : group) {
                Double v = table.get(item);
                if (v != null && v < bestVal) {
                    bestVal = v;
                    best = item;
                }
            }
            if (best != null) {
                inputs.add(best);
            }
        }
        if (inputs.isEmpty()) {
            return false;
        }
        double per = neededTotal / (double) inputs.size();
        boolean changed = false;
        for (Item item : inputs) {
            Double cur = table.get(item);
            if (cur == null || cur + 1.0E-6 < per) {
                table.put(item, Math.max(MIN_UNIT, per));
                changed = true;
            }
        }
        return changed;
    }

    /** Many slabs → 1 block. Skip so cutting recipes (1 block → 6 slabs) win. */
    private static boolean isCompressionRecipe(PricedRecipe recipe) {
        if (recipe.count() != 1 || recipe.ingredientGroups().size() < 2) {
            return false;
        }
        for (List<Item> group : recipe.ingredientGroups()) {
            if (group.isEmpty()) {
                return false;
            }
            boolean anySlab = false;
            for (Item item : group) {
                if (item.builtInRegistryHolder().is(ItemTags.SLABS)) {
                    anySlab = true;
                    break;
                }
            }
            if (!anySlab) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static Double cheapTagValue(Item item) {
        var holder = item.builtInRegistryHolder();
        if (holder.is(ItemTags.LEAVES) || holder.is(ItemTags.FLOWERS) || holder.is(ItemTags.SAPLINGS)) {
            return MIN_UNIT;
        }
        if (holder.is(ItemTags.DIRT) || holder.is(ItemTags.SAND)) {
            return MIN_UNIT;
        }
        return null;
    }

    private static void applyCookingInputs(Level level, Map<Item, Double> table, Set<Item> pinned) {
        RegistryAccess access = level.registryAccess();
        for (Recipe<?> recipe : level.getRecipeManager().getRecipes()) {
            if (!(recipe instanceof AbstractCookingRecipe)) {
                continue;
            }
            ItemStack result = ItemValues.resultOf(recipe, access);
            if (result.isEmpty()) {
                continue;
            }
            Double resultValue = table.get(result.getItem());
            if (resultValue == null) {
                continue;
            }
            double value = resultValue * (double) result.getCount();
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) {
                    continue;
                }
                for (ItemStack stack : ingredient.getItems()) {
                    if (stack.isEmpty()) {
                        continue;
                    }
                    Item input = stack.getItem();
                    if (pinned.contains(input)) {
                        continue;
                    }
                    Double current = table.get(input);
                    if (current == null || value > current) {
                        table.put(input, value);
                    }
                }
            }
        }
    }

    private static List<PricedRecipe> normalize(Level level) {
        RegistryAccess access = level.registryAccess();
        ArrayList<PricedRecipe> normalized = new ArrayList<PricedRecipe>();
        ArrayList<Item> allItems = new ArrayList<Item>(ForgeRegistries.ITEMS.getValues());
        ArrayList<ItemStack> allStacks = null;
        for (Recipe recipe : level.getRecipeManager().getRecipes()) {
            List<List<Item>> groups;
            ItemStack result;
            if (recipe.isSpecial() || (result = ItemValues.resultOf(recipe, access)).isEmpty() || result.getCount() <= 0) continue;
            groups = ItemValues.craftingOptions(recipe);
            if (groups == null || groups.isEmpty()) continue;
            normalized.add(new PricedRecipe(result.getItem(), result.getCount(), groups));
        }
        return normalized;
    }

    @Nullable
    private static List<List<Item>> craftingOptions(Recipe<?> recipe) {
        ArrayList<List<Item>> groups = new ArrayList<List<Item>>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) continue;
            ArrayList<Item> group = new ArrayList<Item>();
            for (ItemStack stack : ingredient.getItems()) {
                if (stack.isEmpty()) continue;
                group.add(stack.getItem());
            }
            if (group.isEmpty()) {
                return null;
            }
            groups.add(group);
        }
        return groups;
    }

    @Nullable

    private static ItemStack resultOf(Recipe<?> recipe, RegistryAccess access) {
        try {
            return recipe.getResultItem();
        }
        catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    @Nullable
    private static Double configOverride(Item item) {
        if (Config.valueOverrides.isEmpty()) {
            return null;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key == null ? null : Config.valueOverrides.get(key.toString());
    }

    private static double rarityFallback(Item item) {
        return switch (item.getDefaultInstance().getRarity()) {
            case UNCOMMON -> 150.0;
            case RARE -> 600.0;
            case EPIC -> 2500.0;
            default -> MIN_UNIT;
        };
    }

    private ItemValues() {
    }

    private record PricedRecipe(Item result, int count, List<List<Item>> ingredientGroups) {
        double cost(Map<Item, Double> table) {
            double sum = 0.0;
            for (List<Item> group : this.ingredientGroups) {
                double cheapest = Double.POSITIVE_INFINITY;
                for (Item item : group) {
                    Double value = table.get(item);
                    if (value == null || !(value < cheapest)) continue;
                    cheapest = value;
                }
                if (Double.isInfinite(cheapest)) {
                    return Double.POSITIVE_INFINITY;
                }
                sum += cheapest;
            }
            return sum <= 0.0 ? Double.POSITIVE_INFINITY : sum / (double)this.count;
        }
    }
}
