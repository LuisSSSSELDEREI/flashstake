package net.execheinz.upgrader.cases;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Pool design (stack value vs case price):
 * - JUNK ≪ price
 * - COMMON < price
 * - UNCOMMON (фиолетовый) ≈ price
 * - RARE (розовый) ≥ price
 * - LEGENDARY (золотой) ≫ price
 * Within a case, higher tier stack values must not undercut lower tiers.
 * Each Item once per case.
 */
public record CaseDefinition(String id, String nameKey, int color, long price, List<CaseEntry> pool) {
    public record CaseEntry(Item item, int count, CaseTier tier) {
    }

    public enum CaseTier {
        JUNK, COMMON, UNCOMMON, RARE, LEGENDARY;

        public int stripColor() {
            return switch (this) {
                case JUNK -> 0xFF9DA1A9;
                case COMMON -> 0xFF4B69FF;
                case UNCOMMON -> 0xFF8847FF;
                case RARE -> 0xFFD32CE6;
                case LEGENDARY -> 0xFFE4AE39;
            };
        }

        public int slotTint() {
            int c = this.stripColor();
            return 0x55000000 | (c & 0x00FFFFFF);
        }
    }

    public static final int ICON_WIDTH = 512;
    public static final int ICON_HEIGHT = 280;

    public String imageName() {
        return this.id;
    }

    public net.minecraft.resources.ResourceLocation icon() {
        return new net.minecraft.resources.ResourceLocation("flashstake", "textures/gui/cases/" + this.imageName() + ".png");
    }

    public static List<CaseDefinition> all() {
        return BY_PRICE;
    }

    public static CaseDefinition byId(String id) {
        for (CaseDefinition c : CASES) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }

    private static CaseDefinition of(String id, String nameKey, int color, long price, CaseEntry... entries) {
        Set<Item> seen = new HashSet<>();
        for (CaseEntry e : entries) {
            if (!seen.add(e.item())) {
                throw new IllegalStateException("Duplicate item in case '" + id + "': " + e.item());
            }
        }
        return new CaseDefinition(id, nameKey, color, price, List.of(entries));
    }

    private static CaseEntry e(Item item, CaseTier tier) {
        return new CaseEntry(item, 1, tier);
    }

    private static CaseEntry e(Item item, int count, CaseTier tier) {
        return new CaseEntry(item, count, tier);
    }

    private static final List<CaseDefinition> CASES = List.of(
        // ~16 / ~32 / ~45 / ~80 / ~140+
        of("dirt", "case.upgrader.dirt", 0x6B8F3C, 40,
            e(Items.DIRT, 16, CaseTier.JUNK), e(Items.GRAVEL, 16, CaseTier.JUNK), e(Items.SAND, 16, CaseTier.JUNK),
            e(Items.COAL, 4, CaseTier.COMMON), e(Items.FLINT, 4, CaseTier.COMMON), e(Items.CLAY_BALL, 4, CaseTier.COMMON),
            e(Items.RAW_IRON, CaseTier.UNCOMMON), e(Items.IRON_NUGGET, 8, CaseTier.UNCOMMON), e(Items.CHARCOAL, 6, CaseTier.UNCOMMON),
            e(Items.IRON_INGOT, 2, CaseTier.RARE), e(Items.GOLD_NUGGET, 12, CaseTier.RARE),
            e(Items.GOLD_INGOT, 2, CaseTier.LEGENDARY), e(Items.EMERALD, CaseTier.LEGENDARY)),

        // ~24 / ~40 / ~55 / ~140 / ~500
        of("wood", "case.upgrader.wood", 0x8B5A2B, 50,
            e(Items.OAK_PLANKS, 12, CaseTier.JUNK), e(Items.STICK, 32, CaseTier.JUNK), e(Items.OAK_SAPLING, 8, CaseTier.JUNK),
            e(Items.APPLE, 8, CaseTier.COMMON), e(Items.CHARCOAL, 6, CaseTier.COMMON), e(Items.BOWL, 8, CaseTier.COMMON),
            e(Items.OAK_LOG, 8, CaseTier.UNCOMMON), e(Items.CHEST, 2, CaseTier.UNCOMMON), e(Items.BOOKSHELF, CaseTier.UNCOMMON),
            e(Items.IRON_INGOT, 3, CaseTier.RARE), e(Items.EMERALD, CaseTier.RARE),
            e(Items.GOLDEN_APPLE, CaseTier.LEGENDARY), e(Items.DIAMOND, CaseTier.LEGENDARY)),

        // ~12 / ~30 / ~80 / ≥100 / ≫100 — price 100, EV well under (junk was ≥60 before)
        of("stone", "case.upgrader.stone", 0x7A7A7A, 100,
            e(Items.COBBLESTONE, 2, CaseTier.JUNK), e(Items.ANDESITE, 2, CaseTier.JUNK), e(Items.GRANITE, 2, CaseTier.JUNK),
            e(Items.STONE, 3, CaseTier.COMMON), e(Items.DIORITE, 3, CaseTier.COMMON), e(Items.COAL, 4, CaseTier.COMMON),
            e(Items.RAW_IRON, 2, CaseTier.UNCOMMON), e(Items.OBSIDIAN, 2, CaseTier.UNCOMMON), e(Items.FLINT, 8, CaseTier.UNCOMMON),
            e(Items.IRON_INGOT, 3, CaseTier.RARE), e(Items.GOLD_INGOT, 2, CaseTier.RARE),
            e(Items.EMERALD, CaseTier.LEGENDARY), e(Items.DIAMOND, CaseTier.LEGENDARY)),

        // ~60 / ~120 / ~240 / ~540 / ~3600
        of("ore", "case.upgrader.ore", 0x4A90D9, 180,
            e(Items.COAL, 8, CaseTier.JUNK), e(Items.RAW_COPPER, 6, CaseTier.JUNK), e(Items.REDSTONE, 12, CaseTier.JUNK),
            e(Items.RAW_IRON, 3, CaseTier.COMMON), e(Items.LAPIS_LAZULI, 12, CaseTier.COMMON), e(Items.RAW_GOLD, 2, CaseTier.COMMON),
            e(Items.IRON_INGOT, 5, CaseTier.UNCOMMON), e(Items.GOLD_INGOT, 4, CaseTier.UNCOMMON), e(Items.EMERALD, CaseTier.UNCOMMON),
            e(Items.DIAMOND, CaseTier.RARE), e(Items.GOLD_BLOCK, CaseTier.RARE),
            e(Items.EMERALD_BLOCK, CaseTier.LEGENDARY), e(Items.DIAMOND_BLOCK, CaseTier.LEGENDARY)),

        // ~40 / ~70 / ~140 / ~400 / ~500+
        of("food", "case.upgrader.food", 0xC45C26, 90,
            e(Items.BREAD, 8, CaseTier.JUNK), e(Items.COOKED_BEEF, 4, CaseTier.JUNK), e(Items.COOKED_PORKCHOP, 4, CaseTier.JUNK),
            e(Items.PUMPKIN_PIE, 4, CaseTier.COMMON), e(Items.COOKED_SALMON, 8, CaseTier.COMMON), e(Items.CAKE, CaseTier.COMMON),
            e(Items.GOLDEN_CARROT, 4, CaseTier.UNCOMMON), e(Items.GOLD_INGOT, 2, CaseTier.UNCOMMON), e(Items.HONEY_BOTTLE, 4, CaseTier.UNCOMMON),
            e(Items.EMERALD, 2, CaseTier.RARE), e(Items.DIAMOND, CaseTier.RARE),
            e(Items.GOLDEN_APPLE, CaseTier.LEGENDARY), e(Items.GOLD_BLOCK, CaseTier.LEGENDARY)),

        // ~50 / ~100 / ~150 / ~350 / ~1500
        of("mob", "case.upgrader.mob", 0x8B0000, 160,
            e(Items.ROTTEN_FLESH, 24, CaseTier.JUNK), e(Items.BONE, 12, CaseTier.JUNK), e(Items.STRING, 12, CaseTier.JUNK),
            e(Items.GUNPOWDER, 4, CaseTier.COMMON), e(Items.SPIDER_EYE, 8, CaseTier.COMMON), e(Items.SLIME_BALL, 8, CaseTier.COMMON),
            e(Items.BLAZE_ROD, 3, CaseTier.UNCOMMON), e(Items.ENDER_PEARL, 3, CaseTier.UNCOMMON), e(Items.MAGMA_CREAM, 4, CaseTier.UNCOMMON),
            e(Items.GHAST_TEAR, 2, CaseTier.RARE), e(Items.SHULKER_SHELL, CaseTier.RARE),
            e(Items.TOTEM_OF_UNDYING, CaseTier.LEGENDARY), e(Items.EMERALD_BLOCK, CaseTier.LEGENDARY)),

        // ~50 / ~150 / ~400 / ~1200 / ~4800
        of("nether", "case.upgrader.nether", 0x6B1E1E, 280,
            e(Items.NETHERRACK, 32, CaseTier.JUNK), e(Items.SOUL_SAND, 12, CaseTier.JUNK), e(Items.MAGMA_BLOCK, 8, CaseTier.JUNK),
            e(Items.QUARTZ, 12, CaseTier.COMMON), e(Items.GLOWSTONE, 4, CaseTier.COMMON), e(Items.BLAZE_ROD, 3, CaseTier.COMMON),
            e(Items.GOLD_INGOT, 4, CaseTier.UNCOMMON), e(Items.OBSIDIAN, 6, CaseTier.UNCOMMON), e(Items.DIAMOND, CaseTier.UNCOMMON),
            e(Items.CRYING_OBSIDIAN, 8, CaseTier.RARE), e(Items.GOLD_BLOCK, 2, CaseTier.RARE),
            e(Items.NETHERITE_SCRAP, CaseTier.LEGENDARY), e(Items.ANCIENT_DEBRIS, CaseTier.LEGENDARY)),

        // ~40 / ~100 / ~300 / ~400 / ~900+
        of("ocean", "case.upgrader.ocean", 0x1E90FF, 200,
            e(Items.COD, 12, CaseTier.JUNK), e(Items.KELP, 24, CaseTier.JUNK), e(Items.PRISMARINE_SHARD, 12, CaseTier.JUNK),
            e(Items.PRISMARINE_CRYSTALS, 8, CaseTier.COMMON), e(Items.COOKED_COD, 12, CaseTier.COMMON), e(Items.NAUTILUS_SHELL, 2, CaseTier.COMMON),
            e(Items.HEART_OF_THE_SEA, CaseTier.UNCOMMON), e(Items.EMERALD, 2, CaseTier.UNCOMMON), e(Items.SPONGE, CaseTier.UNCOMMON),
            e(Items.DIAMOND, CaseTier.RARE), e(Items.TURTLE_HELMET, 2, CaseTier.RARE),
            e(Items.TRIDENT, CaseTier.LEGENDARY), e(Items.CONDUIT, CaseTier.LEGENDARY)),

        // ~60 / ~120 / ~200 / ~400 / ~540+
        of("redstone", "case.upgrader.redstone", 0xB22222, 170,
            e(Items.REDSTONE, 16, CaseTier.JUNK), e(Items.REPEATER, 4, CaseTier.JUNK), e(Items.REDSTONE_TORCH, 12, CaseTier.JUNK),
            e(Items.COMPARATOR, 2, CaseTier.COMMON), e(Items.PISTON, 3, CaseTier.COMMON), e(Items.OBSERVER, 2, CaseTier.COMMON),
            e(Items.HOPPER, 2, CaseTier.UNCOMMON), e(Items.GOLD_INGOT, 3, CaseTier.UNCOMMON), e(Items.SLIME_BLOCK, 4, CaseTier.UNCOMMON),
            e(Items.DIAMOND, CaseTier.RARE), e(Items.EMERALD, 3, CaseTier.RARE),
            e(Items.GOLD_BLOCK, CaseTier.LEGENDARY), e(Items.DIAMOND_BLOCK, CaseTier.LEGENDARY)),

        // iron tools junk/common ~ craft cost; diamond mid; scrap/gold end
        of("tools", "case.upgrader.tools", 0xA0A0A0, 240,
            e(Items.IRON_SHOVEL, CaseTier.JUNK), e(Items.IRON_HOE, CaseTier.JUNK), e(Items.IRON_AXE, CaseTier.JUNK),
            e(Items.IRON_PICKAXE, CaseTier.COMMON), e(Items.IRON_SWORD, CaseTier.COMMON), e(Items.SHIELD, CaseTier.COMMON),
            e(Items.DIAMOND_SHOVEL, CaseTier.UNCOMMON), e(Items.DIAMOND_AXE, CaseTier.UNCOMMON), e(Items.DIAMOND, CaseTier.UNCOMMON),
            e(Items.DIAMOND_PICKAXE, CaseTier.RARE), e(Items.DIAMOND_SWORD, CaseTier.RARE),
            e(Items.NETHERITE_SCRAP, CaseTier.LEGENDARY), e(Items.GOLD_BLOCK, 2, CaseTier.LEGENDARY)),

        of("armor", "case.upgrader.armor", 0x4169E1, 260,
            e(Items.IRON_HELMET, CaseTier.JUNK), e(Items.IRON_BOOTS, CaseTier.JUNK), e(Items.IRON_LEGGINGS, CaseTier.JUNK),
            e(Items.IRON_CHESTPLATE, CaseTier.COMMON), e(Items.GOLDEN_CHESTPLATE, CaseTier.COMMON), e(Items.SHIELD, CaseTier.COMMON),
            e(Items.DIAMOND_HELMET, CaseTier.UNCOMMON), e(Items.DIAMOND_BOOTS, CaseTier.UNCOMMON), e(Items.DIAMOND, CaseTier.UNCOMMON),
            e(Items.DIAMOND_LEGGINGS, CaseTier.RARE), e(Items.DIAMOND_HORSE_ARMOR, CaseTier.RARE),
            e(Items.DIAMOND_CHESTPLATE, CaseTier.LEGENDARY), e(Items.NETHERITE_SCRAP, CaseTier.LEGENDARY)),

        // ~50 / ~80 / ~140 / ~500 / ~540+
        of("farm", "case.upgrader.farm", 0x228B22, 75,
            e(Items.WHEAT, 16, CaseTier.JUNK), e(Items.CARROT, 12, CaseTier.JUNK), e(Items.POTATO, 12, CaseTier.JUNK),
            e(Items.BEETROOT, 16, CaseTier.COMMON), e(Items.BONE_MEAL, 24, CaseTier.COMMON), e(Items.BREAD, 12, CaseTier.COMMON),
            e(Items.GOLDEN_CARROT, 4, CaseTier.UNCOMMON), e(Items.HAY_BLOCK, 4, CaseTier.UNCOMMON), e(Items.GOLD_INGOT, 2, CaseTier.UNCOMMON),
            e(Items.EMERALD, CaseTier.RARE), e(Items.GOLDEN_APPLE, CaseTier.RARE),
            e(Items.DIAMOND, CaseTier.LEGENDARY), e(Items.GOLD_BLOCK, CaseTier.LEGENDARY)),

        // ~50 / ~100 / ~400 / ~600 / ~1500
        of("hunter", "case.upgrader.hunter", 0x556B2F, 220,
            e(Items.ARROW, 32, CaseTier.JUNK), e(Items.LEATHER, 8, CaseTier.JUNK), e(Items.FEATHER, 12, CaseTier.JUNK),
            e(Items.BOW, CaseTier.COMMON), e(Items.SPECTRAL_ARROW, 16, CaseTier.COMMON), e(Items.COOKED_BEEF, 12, CaseTier.COMMON),
            e(Items.CROSSBOW, CaseTier.UNCOMMON), e(Items.DIAMOND, CaseTier.UNCOMMON), e(Items.LEAD, 8, CaseTier.UNCOMMON),
            e(Items.NAME_TAG, CaseTier.RARE), e(Items.SADDLE, CaseTier.RARE),
            e(Items.TOTEM_OF_UNDYING, CaseTier.LEGENDARY), e(Items.EMERALD_BLOCK, CaseTier.LEGENDARY)),

        // ~50 / ~150 / ~400 / ~750 / ~2500
        of("end", "case.upgrader.end", 0x4B0082, 400,
            e(Items.END_STONE, 16, CaseTier.JUNK), e(Items.CHORUS_FRUIT, 8, CaseTier.JUNK), e(Items.ENDER_PEARL, CaseTier.JUNK),
            e(Items.PURPUR_BLOCK, 8, CaseTier.COMMON), e(Items.END_ROD, 6, CaseTier.COMMON), e(Items.DRAGON_BREATH, 2, CaseTier.COMMON),
            e(Items.DIAMOND, CaseTier.UNCOMMON), e(Items.SHULKER_SHELL, CaseTier.UNCOMMON), e(Items.ENDER_EYE, 8, CaseTier.UNCOMMON),
            e(Items.EMERALD_BLOCK, CaseTier.RARE), e(Items.SHULKER_BOX, CaseTier.RARE),
            e(Items.ELYTRA, CaseTier.LEGENDARY), e(Items.NETHERITE_INGOT, CaseTier.LEGENDARY)),

        // ~280 / ~500 / ~1260 / ~1500 / ~4800
        of("treasure", "case.upgrader.treasure", 0xDAA520, 550,
            e(Items.GOLD_INGOT, 4, CaseTier.JUNK), e(Items.EMERALD, 2, CaseTier.JUNK), e(Items.IRON_BLOCK, CaseTier.JUNK),
            e(Items.DIAMOND, CaseTier.COMMON), e(Items.GOLDEN_APPLE, CaseTier.COMMON), e(Items.GOLD_BLOCK, CaseTier.COMMON),
            e(Items.EMERALD_BLOCK, CaseTier.UNCOMMON), e(Items.HEART_OF_THE_SEA, 2, CaseTier.UNCOMMON), e(Items.DIAMOND_HORSE_ARMOR, CaseTier.UNCOMMON),
            e(Items.NETHERITE_SCRAP, CaseTier.RARE), e(Items.TOTEM_OF_UNDYING, CaseTier.RARE),
            e(Items.ENCHANTED_GOLDEN_APPLE, CaseTier.LEGENDARY), e(Items.NETHERITE_INGOT, CaseTier.LEGENDARY)),

        // ~200 / ~800 / ~1200 / ~1500 / ~4800+
        of("netherite", "case.upgrader.netherite", 0x3D3D3D, 1000,
            e(Items.NETHER_BRICK, 24, CaseTier.JUNK), e(Items.GOLD_INGOT, 6, CaseTier.JUNK), e(Items.QUARTZ, 24, CaseTier.JUNK),
            e(Items.QUARTZ_BLOCK, 8, CaseTier.COMMON), e(Items.GOLD_BLOCK, CaseTier.COMMON), e(Items.DIAMOND, 2, CaseTier.COMMON),
            e(Items.NETHERITE_SCRAP, CaseTier.UNCOMMON), e(Items.ANCIENT_DEBRIS, CaseTier.UNCOMMON), e(Items.DIAMOND_BLOCK, CaseTier.UNCOMMON),
            e(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, CaseTier.RARE), e(Items.TOTEM_OF_UNDYING, CaseTier.RARE),
            e(Items.NETHERITE_INGOT, CaseTier.LEGENDARY), e(Items.NETHERITE_SWORD, CaseTier.LEGENDARY)),

        // ~500–800 / ~1200–1500 / ~2500–3600 / ~4800+ / ~5500–7000
        of("luxury", "case.upgrader.luxury", 0xFFD700, 1500,
            e(Items.DIAMOND, 2, CaseTier.JUNK), e(Items.EMERALD, 4, CaseTier.JUNK), e(Items.GOLDEN_APPLE, CaseTier.JUNK),
            e(Items.NETHERITE_SCRAP, CaseTier.COMMON), e(Items.TOTEM_OF_UNDYING, CaseTier.COMMON), e(Items.SHULKER_BOX, 2, CaseTier.COMMON),
            e(Items.ENCHANTED_GOLDEN_APPLE, CaseTier.UNCOMMON), e(Items.DIAMOND_BLOCK, CaseTier.UNCOMMON), e(Items.ANCIENT_DEBRIS, 2, CaseTier.UNCOMMON),
            e(Items.NETHERITE_INGOT, CaseTier.RARE), e(Items.NETHERITE_HELMET, CaseTier.RARE),
            e(Items.ELYTRA, CaseTier.LEGENDARY), e(Items.NETHER_STAR, CaseTier.LEGENDARY)),

        // ~100 / ~160 / ~400 / ~500 / ~1500
        of("chaos", "case.upgrader.chaos", 0xFF00FF, 350,
            e(Items.GUNPOWDER, 8, CaseTier.JUNK), e(Items.FIRE_CHARGE, 8, CaseTier.JUNK), e(Items.TNT, CaseTier.JUNK),
            e(Items.BLAZE_POWDER, 8, CaseTier.COMMON), e(Items.BONE, 24, CaseTier.COMMON), e(Items.OBSIDIAN, 4, CaseTier.COMMON),
            e(Items.END_CRYSTAL, CaseTier.UNCOMMON), e(Items.DIAMOND, CaseTier.UNCOMMON), e(Items.CRYING_OBSIDIAN, 4, CaseTier.UNCOMMON),
            e(Items.RESPAWN_ANCHOR, CaseTier.RARE), e(Items.ANCIENT_DEBRIS, CaseTier.RARE),
            e(Items.TOTEM_OF_UNDYING, CaseTier.LEGENDARY), e(Items.WITHER_SKELETON_SKULL, CaseTier.LEGENDARY)),

        // ~50 / ~100 / ~200 / ~400 / ~1260
        of("builder", "case.upgrader.builder", 0x708090, 120,
            e(Items.BRICKS, 16, CaseTier.JUNK), e(Items.GLASS, 16, CaseTier.JUNK), e(Items.WHITE_CONCRETE, 16, CaseTier.JUNK),
            e(Items.QUARTZ_BLOCK, 4, CaseTier.COMMON), e(Items.GLOWSTONE, 2, CaseTier.COMMON), e(Items.SEA_LANTERN, CaseTier.COMMON),
            e(Items.GOLD_INGOT, 3, CaseTier.UNCOMMON), e(Items.OBSIDIAN, 4, CaseTier.UNCOMMON), e(Items.IRON_BLOCK, CaseTier.UNCOMMON),
            e(Items.DIAMOND, CaseTier.RARE), e(Items.EMERALD, 3, CaseTier.RARE),
            e(Items.GOLD_BLOCK, CaseTier.LEGENDARY), e(Items.EMERALD_BLOCK, CaseTier.LEGENDARY)),

        // fragments cheap; echo mid-common; scrap/totem/ingot climb
        of("mystery", "case.upgrader.mystery", 0x2F4F4F, 480,
            e(Items.AMETHYST_SHARD, 8, CaseTier.JUNK), e(Items.DISC_FRAGMENT_5, 3, CaseTier.JUNK), e(Items.MUSIC_DISC_5, CaseTier.JUNK),
            e(Items.ECHO_SHARD, CaseTier.COMMON), e(Items.GOLDEN_APPLE, CaseTier.COMMON), e(Items.RECOVERY_COMPASS, CaseTier.COMMON),
            e(Items.DIAMOND, CaseTier.UNCOMMON), e(Items.NETHERITE_SCRAP, CaseTier.UNCOMMON), e(Items.EMERALD, 3, CaseTier.UNCOMMON),
            e(Items.TOTEM_OF_UNDYING, CaseTier.RARE), e(Items.DIAMOND_BLOCK, CaseTier.RARE),
            e(Items.ENCHANTED_GOLDEN_APPLE, CaseTier.LEGENDARY), e(Items.NETHERITE_INGOT, CaseTier.LEGENDARY))
    );

    /** Cheapest → most expensive for the cases grid. */
    private static final List<CaseDefinition> BY_PRICE = CASES.stream()
        .sorted(Comparator.comparingLong(CaseDefinition::price))
        .toList();
}
