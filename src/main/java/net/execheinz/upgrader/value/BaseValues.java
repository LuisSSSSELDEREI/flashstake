/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Items
 */
package net.execheinz.upgrader.value;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class BaseValues {
    private static final Map<Item, Double> VALUES = new HashMap<Item, Double>();
    private static final Set<Item> BLACKLIST = new HashSet<Item>();

    private static void put(double value, Item ... items) {
        for (Item item : items) {
            VALUES.put(item, value);
        }
    }

    public static Double get(Item item) {
        return VALUES.get(item);
    }

    public static boolean isBlacklisted(Item item) {
        return BLACKLIST.contains(item);
    }

    private BaseValues() {
    }

    static {
        // Cobble / filler stone ?????? must be ?????? 6?"???slab (1 block ???-?? 6 slabs), else craft prints money
        BaseValues.put(1.0, Items.DIRT, Items.SAND, Items.RED_SAND, Items.GRAVEL, Items.NETHERRACK, Items.END_STONE, Items.TUFF, Items.CALCITE, Items.BASALT, Items.SNOWBALL, Items.WHEAT_SEEDS, Items.KELP);
        // Cobble ?????? easy to mine; ??????2 so 3 cobble ???-?? 6 slabs @1 don't print
        BaseValues.put(2.0, Items.COBBLESTONE, Items.COBBLED_DEEPSLATE);
        BaseValues.put(4.0, Items.GRANITE, Items.DIORITE, Items.ANDESITE, Items.BLACKSTONE);
        // Very cheap / freely farmable foliage & filler (floor = 1)
        BaseValues.put(1.0,
            Items.GRASS, Items.FERN, Items.SEAGRASS, Items.TALL_GRASS, Items.LARGE_FERN,
            Items.VINE, Items.GLOW_LICHEN, Items.HANGING_ROOTS, Items.MOSS_CARPET,
            Items.LILY_PAD, Items.SUGAR_CANE, Items.BAMBOO
        );
        BaseValues.put(2.0, Items.DEAD_BUSH, Items.FLINT);
        BaseValues.put(1.0,
            Items.GRASS_BLOCK, Items.PODZOL, Items.MYCELIUM, Items.DIRT_PATH, Items.ROOTED_DIRT, Items.MUD,
            Items.OAK_LEAVES, Items.SPRUCE_LEAVES, Items.BIRCH_LEAVES, Items.JUNGLE_LEAVES, Items.ACACIA_LEAVES,
            Items.DARK_OAK_LEAVES, Items.MANGROVE_LEAVES, Items.AZALEA_LEAVES, Items.FLOWERING_AZALEA_LEAVES,
            Items.OAK_SAPLING, Items.SPRUCE_SAPLING, Items.BIRCH_SAPLING, Items.JUNGLE_SAPLING, Items.ACACIA_SAPLING,
            Items.DARK_OAK_SAPLING, Items.MANGROVE_PROPAGULE, Items.AZALEA, Items.FLOWERING_AZALEA,
            Items.DANDELION, Items.POPPY, Items.BLUE_ORCHID, Items.ALLIUM, Items.AZURE_BLUET, Items.RED_TULIP,
            Items.ORANGE_TULIP, Items.WHITE_TULIP, Items.PINK_TULIP, Items.OXEYE_DAISY, Items.CORNFLOWER,
            Items.LILY_OF_THE_VALLEY, Items.SUNFLOWER, Items.LILAC, Items.ROSE_BUSH, Items.PEONY,
            Items.BROWN_MUSHROOM, Items.RED_MUSHROOM, Items.CRIMSON_FUNGUS, Items.WARPED_FUNGUS,
            Items.CRIMSON_ROOTS, Items.WARPED_ROOTS, Items.NETHER_SPROUTS, Items.WEEPING_VINES, Items.TWISTING_VINES,
            Items.SNOW, Items.ICE, Items.PACKED_ICE, Items.BLUE_ICE, Items.CLAY, Items.SNOW_BLOCK
        );
        // Stone family ?????? above cobble; still ?????? 6?"???slab
        BaseValues.put(8.0, Items.STONE, Items.DEEPSLATE, Items.CACTUS, Items.SOUL_SAND, Items.SOUL_SOIL, Items.CLAY_BALL, Items.MOSS_BLOCK);
        // Obsidian is mid-game (diamond pick) ?????? not dirt-tier
        BaseValues.put(40.0, Items.OBSIDIAN);
        BaseValues.put(80.0, Items.CRYING_OBSIDIAN);
        BaseValues.put(500.0, Items.RESPAWN_ANCHOR);
        // 1 log = 4 planks; 3 planks ???-?? 6 slabs ???-?? plank ?????? 2?"???slab, log ?????? 8?"???slab
        BaseValues.put(8.0,
            Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.JUNGLE_LOG, Items.ACACIA_LOG, Items.DARK_OAK_LOG,
            Items.MANGROVE_LOG, Items.CRIMSON_STEM, Items.WARPED_STEM,
            Items.STRIPPED_OAK_LOG, Items.STRIPPED_SPRUCE_LOG, Items.STRIPPED_BIRCH_LOG, Items.STRIPPED_JUNGLE_LOG,
            Items.STRIPPED_ACACIA_LOG, Items.STRIPPED_DARK_OAK_LOG, Items.STRIPPED_MANGROVE_LOG,
            Items.STRIPPED_CRIMSON_STEM, Items.STRIPPED_WARPED_STEM,
            Items.OAK_WOOD, Items.SPRUCE_WOOD, Items.BIRCH_WOOD, Items.JUNGLE_WOOD, Items.ACACIA_WOOD, Items.DARK_OAK_WOOD,
            Items.MANGROVE_WOOD, Items.CRIMSON_HYPHAE, Items.WARPED_HYPHAE,
            Items.STRIPPED_OAK_WOOD, Items.STRIPPED_SPRUCE_WOOD, Items.STRIPPED_BIRCH_WOOD, Items.STRIPPED_JUNGLE_WOOD,
            Items.STRIPPED_ACACIA_WOOD, Items.STRIPPED_DARK_OAK_WOOD, Items.STRIPPED_MANGROVE_WOOD,
            Items.STRIPPED_CRIMSON_HYPHAE, Items.STRIPPED_WARPED_HYPHAE
        );
        BaseValues.put(2.0,
            Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS, Items.JUNGLE_PLANKS, Items.ACACIA_PLANKS,
            Items.DARK_OAK_PLANKS, Items.MANGROVE_PLANKS,
            Items.CRIMSON_PLANKS, Items.WARPED_PLANKS
        );
        // Bamboo block ???-?? 2 planks
        // Slabs: priced as half of block in ItemValues.enforceBlockFamilyPricing (not flat 1)
        BaseValues.put(3.0, Items.WHEAT, Items.POTATO, Items.CARROT, Items.BEETROOT, Items.APPLE, Items.EGG);
        BaseValues.put(6.0, Items.PORKCHOP, Items.BEEF, Items.CHICKEN, Items.MUTTON, Items.COD, Items.SALMON, Items.RABBIT);
        // Cooked slightly above raw (was falling to 1); small premium only.
        BaseValues.put(8.0, Items.COOKED_PORKCHOP, Items.COOKED_BEEF, Items.COOKED_CHICKEN, Items.COOKED_MUTTON, Items.COOKED_COD, Items.COOKED_SALMON, Items.COOKED_RABBIT);
        BaseValues.put(5.0, Items.BAKED_POTATO);

        BaseValues.put(30.0, Items.HONEYCOMB, Items.PUFFERFISH, Items.CHORUS_FRUIT);
        BaseValues.put(4.0, Items.STRING, Items.FEATHER, Items.BONE, Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.INK_SAC, Items.COBWEB);
        BaseValues.put(12.0, Items.LEATHER, Items.SLIME_BALL, Items.RABBIT_HIDE, Items.PRISMARINE_SHARD, Items.PRISMARINE_CRYSTALS);
        BaseValues.put(20.0, Items.GUNPOWDER, Items.GLOW_INK_SAC);
        // Magma: nether-mined block is cheap; cream from cubes (~4). Was 20 and inflated MAGMA_BLOCK via 4x craft.
        BaseValues.put(4.0, Items.MAGMA_CREAM);
        BaseValues.put(3.0, Items.MAGMA_BLOCK);
        BaseValues.put(50.0, Items.ENDER_PEARL, Items.BLAZE_ROD, Items.PHANTOM_MEMBRANE, Items.NAUTILUS_SHELL);
        // Eye = pearl + blaze powder (~75). Tear must be ?????? crystal ?????? eye ?????? glass or crystal craft is free money.
        BaseValues.put(75.0, Items.ENDER_EYE);
        BaseValues.put(400.0, Items.GHAST_TEAR);
        BaseValues.put(120.0, Items.RABBIT_FOOT);
        // End-game storage ?????? must be pinned or undyed/coloured boxes fall to ~1
        BaseValues.put(350.0, Items.SHULKER_SHELL);
        BaseValues.put(750.0,
            Items.SHULKER_BOX,
            Items.WHITE_SHULKER_BOX, Items.ORANGE_SHULKER_BOX, Items.MAGENTA_SHULKER_BOX,
            Items.LIGHT_BLUE_SHULKER_BOX, Items.YELLOW_SHULKER_BOX, Items.LIME_SHULKER_BOX,
            Items.PINK_SHULKER_BOX, Items.GRAY_SHULKER_BOX, Items.LIGHT_GRAY_SHULKER_BOX,
            Items.CYAN_SHULKER_BOX, Items.PURPLE_SHULKER_BOX, Items.BLUE_SHULKER_BOX,
            Items.BROWN_SHULKER_BOX, Items.GREEN_SHULKER_BOX, Items.RED_SHULKER_BOX,
            Items.BLACK_SHULKER_BOX
        );
        BaseValues.put(100.0, Items.TNT);
        BaseValues.put(18.0, Items.FIRE_CHARGE);
        BaseValues.put(400.0, Items.END_CRYSTAL);
        BaseValues.put(900.0, Items.CONDUIT);
        // Wither skulls gate nether star / wither — raise vs farmable drops
        BaseValues.put(2200.0, Items.WITHER_SKELETON_SKULL);
        BaseValues.put(500.0, Items.GOLDEN_APPLE);
        BaseValues.put(4800.0, Items.NETHERITE_INGOT);
        BaseValues.put(360.0, Items.IRON_BLOCK);
        // Anvil = 3 iron blocks + 4 ingots (=1240). Chipped/damaged have no craft -> were rarityFallback=1
        BaseValues.put(1240.0, Items.ANVIL);
        BaseValues.put(900.0, Items.CHIPPED_ANVIL);
        BaseValues.put(600.0, Items.DAMAGED_ANVIL);

        BaseValues.put(540.0, Items.GOLD_BLOCK);
        BaseValues.put(1260.0, Items.EMERALD_BLOCK);
        BaseValues.put(3600.0, Items.DIAMOND_BLOCK);
        BaseValues.put(90.0, Items.LAPIS_BLOCK);
        BaseValues.put(72.0, Items.REDSTONE_BLOCK);
        BaseValues.put(108.0, Items.COPPER_BLOCK,
            Items.EXPOSED_COPPER, Items.WEATHERED_COPPER, Items.OXIDIZED_COPPER,
            Items.CUT_COPPER, Items.EXPOSED_CUT_COPPER, Items.WEATHERED_CUT_COPPER, Items.OXIDIZED_CUT_COPPER,
            Items.WAXED_COPPER_BLOCK, Items.WAXED_EXPOSED_COPPER, Items.WAXED_WEATHERED_COPPER, Items.WAXED_OXIDIZED_COPPER,
            Items.WAXED_CUT_COPPER, Items.WAXED_EXPOSED_CUT_COPPER, Items.WAXED_WEATHERED_CUT_COPPER, Items.WAXED_OXIDIZED_CUT_COPPER
        );
        // Stairs = block + 1; slabs = half block (set in ItemValues)
        BaseValues.put(109.0,
            Items.CUT_COPPER_STAIRS, Items.EXPOSED_CUT_COPPER_STAIRS, Items.WEATHERED_CUT_COPPER_STAIRS, Items.OXIDIZED_CUT_COPPER_STAIRS,
            Items.WAXED_CUT_COPPER_STAIRS, Items.WAXED_EXPOSED_CUT_COPPER_STAIRS, Items.WAXED_WEATHERED_CUT_COPPER_STAIRS, Items.WAXED_OXIDIZED_CUT_COPPER_STAIRS
        );
        BaseValues.put(48.0, Items.QUARTZ_BLOCK);
        BaseValues.put(32.0, Items.GLOWSTONE);
        BaseValues.put(6.0, Items.COAL, Items.CHARCOAL);
        BaseValues.put(8.0, Items.REDSTONE, Items.GLOWSTONE_DUST);
        BaseValues.put(10.0, Items.LAPIS_LAZULI, Items.RAW_COPPER);
        BaseValues.put(12.0, Items.COPPER_INGOT, Items.QUARTZ, Items.AMETHYST_SHARD);
        // Ore sell ?????? drop count ?"??? drop value (buy ore ???-?? mine ???-?? sell print)
        BaseValues.put(70.0, Items.REDSTONE_ORE, Items.DEEPSLATE_REDSTONE_ORE);
        BaseValues.put(90.0, Items.LAPIS_ORE, Items.DEEPSLATE_LAPIS_ORE);
        // Copper ore drops ~2??????5 raw copper (10 each) ?????? pin ?????? 5?"???raw
        BaseValues.put(50.0, Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE);
        BaseValues.put(35.0, Items.RAW_IRON);
        BaseValues.put(40.0, Items.IRON_INGOT);
        // Empty bucket ?????? 3?"???iron; filled must be ?????? empty or pour???-??sell prints
        BaseValues.put(120.0, Items.BUCKET);
        BaseValues.put(125.0,
            Items.WATER_BUCKET, Items.LAVA_BUCKET, Items.POWDER_SNOW_BUCKET, Items.MILK_BUCKET,
            Items.AXOLOTL_BUCKET, Items.COD_BUCKET, Items.SALMON_BUCKET,
            Items.TROPICAL_FISH_BUCKET, Items.PUFFERFISH_BUCKET, Items.TADPOLE_BUCKET
        );
        BaseValues.put(55.0, Items.RAW_GOLD);
        BaseValues.put(60.0, Items.GOLD_INGOT);
        BaseValues.put(140.0, Items.EMERALD);
        BaseValues.put(400.0, Items.DIAMOND);
        BaseValues.put(1200.0, Items.ANCIENT_DEBRIS, Items.NETHERITE_SCRAP);
        // Echo / recovery ?????? was too juicy in mystery case & market
        BaseValues.put(80.0, Items.ECHO_SHARD);
        BaseValues.put(120.0, Items.RECOVERY_COMPASS);
        BaseValues.put(300.0, Items.HEART_OF_THE_SEA, Items.SPONGE);
        BaseValues.put(600.0, Items.SADDLE, Items.NAME_TAG);
        // Horse armor is uncraftable (except leather) - without pins it falls to rarityFallback=1
        BaseValues.put(80.0, Items.LEATHER_HORSE_ARMOR);
        BaseValues.put(250.0, Items.IRON_HORSE_ARMOR);
        BaseValues.put(400.0, Items.GOLDEN_HORSE_ARMOR);
        BaseValues.put(1200.0, Items.DIAMOND_HORSE_ARMOR);
        // Template dupe: 1 template + diamond + mat ???-?? 2 templates ???-?? V ?????? diamond+mat ?????? 401
        BaseValues.put(50.0
        );
        BaseValues.put(25.0,
            Items.FLOWER_BANNER_PATTERN, Items.CREEPER_BANNER_PATTERN, Items.SKULL_BANNER_PATTERN,
            Items.MOJANG_BANNER_PATTERN, Items.GLOBE_BANNER_PATTERN, Items.PIGLIN_BANNER_PATTERN
        );
        BaseValues.put(5.0, Items.MAP);
        // Glass: sand=1 ???-?? smelt print if glass stays ~1
        BaseValues.put(6.0,
            Items.GLASS,
            Items.WHITE_STAINED_GLASS, Items.ORANGE_STAINED_GLASS, Items.MAGENTA_STAINED_GLASS,
            Items.LIGHT_BLUE_STAINED_GLASS, Items.YELLOW_STAINED_GLASS, Items.LIME_STAINED_GLASS,
            Items.PINK_STAINED_GLASS, Items.GRAY_STAINED_GLASS, Items.LIGHT_GRAY_STAINED_GLASS,
            Items.CYAN_STAINED_GLASS, Items.PURPLE_STAINED_GLASS, Items.BLUE_STAINED_GLASS,
            Items.BROWN_STAINED_GLASS, Items.GREEN_STAINED_GLASS, Items.RED_STAINED_GLASS, Items.BLACK_STAINED_GLASS,
            Items.TINTED_GLASS
        );
        BaseValues.put(2.0,
            Items.GLASS_PANE,
            Items.WHITE_STAINED_GLASS_PANE, Items.ORANGE_STAINED_GLASS_PANE, Items.MAGENTA_STAINED_GLASS_PANE,
            Items.LIGHT_BLUE_STAINED_GLASS_PANE, Items.YELLOW_STAINED_GLASS_PANE, Items.LIME_STAINED_GLASS_PANE,
            Items.PINK_STAINED_GLASS_PANE, Items.GRAY_STAINED_GLASS_PANE, Items.LIGHT_GRAY_STAINED_GLASS_PANE,
            Items.CYAN_STAINED_GLASS_PANE, Items.PURPLE_STAINED_GLASS_PANE, Items.BLUE_STAINED_GLASS_PANE,
            Items.BROWN_STAINED_GLASS_PANE, Items.GREEN_STAINED_GLASS_PANE, Items.RED_STAINED_GLASS_PANE, Items.BLACK_STAINED_GLASS_PANE
        );
        // Melon block ???-?? 3??????7 slices; pumpkin ???-?? 4 seeds
        BaseValues.put(2.0, Items.MELON_SLICE);
        BaseValues.put(20.0, Items.MELON);
        BaseValues.put(10.0, Items.PUMPKIN);
        BaseValues.put(5.0, Items.CARVED_PUMPKIN);
        BaseValues.put(1.0, Items.PUMPKIN_SEEDS, Items.MELON_SEEDS);
        // Music: 9?"???fragment ???-?? disc_5; pin both so rarity-fallback (600) can't print
        BaseValues.put(10.0, Items.DISC_FRAGMENT_5);
        BaseValues.put(90.0,
            Items.MUSIC_DISC_13, Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_BLOCKS, Items.MUSIC_DISC_CHIRP,
            Items.MUSIC_DISC_FAR, Items.MUSIC_DISC_MALL, Items.MUSIC_DISC_MELLOHI, Items.MUSIC_DISC_STAL,
            Items.MUSIC_DISC_STRAD, Items.MUSIC_DISC_WARD, Items.MUSIC_DISC_11, Items.MUSIC_DISC_WAIT,
            Items.MUSIC_DISC_OTHERSIDE, Items.MUSIC_DISC_5, Items.MUSIC_DISC_PIGSTEP
        );
        BaseValues.put(40.0, Items.DRAGON_BREATH);
        BaseValues.put(25.0, Items.SCUTE);
        BaseValues.put(125.0, Items.TURTLE_HELMET);
        BaseValues.put(1500.0, Items.TOTEM_OF_UNDYING, Items.TRIDENT);
        BaseValues.put(2500.0, Items.ENCHANTED_GOLDEN_APPLE);
        // Above netherite ingot so case rarity ladders stay valid
        BaseValues.put(5500.0, Items.ELYTRA);
        // Above netherite gear so legendary case tiers stay above rare
        BaseValues.put(7000.0, Items.NETHER_STAR);
        BaseValues.put(20000.0, Items.DRAGON_EGG);
        BLACKLIST.add(Items.BEDROCK);
        BLACKLIST.add(Items.BARRIER);
        BLACKLIST.add(Items.LIGHT);
        BLACKLIST.add(Items.STRUCTURE_BLOCK);
        BLACKLIST.add(Items.STRUCTURE_VOID);
        BLACKLIST.add(Items.JIGSAW);
        BLACKLIST.add(Items.COMMAND_BLOCK);
        BLACKLIST.add(Items.CHAIN_COMMAND_BLOCK);
        BLACKLIST.add(Items.REPEATING_COMMAND_BLOCK);
        BLACKLIST.add(Items.COMMAND_BLOCK_MINECART);
        BLACKLIST.add(Items.DEBUG_STICK);
        BLACKLIST.add(Items.KNOWLEDGE_BOOK);
        BLACKLIST.add(Items.SPAWNER);
        BLACKLIST.add(Items.BUDDING_AMETHYST);
        BLACKLIST.add(Items.END_PORTAL_FRAME);
        BLACKLIST.add(Items.REINFORCED_DEEPSLATE);
        BLACKLIST.add(Items.PETRIFIED_OAK_SLAB);
        BLACKLIST.add(Items.FARMLAND);
        // DIRTB_PATH removed from blacklist ?????? priced cheap above
        BLACKLIST.add(Items.INFESTED_STONE);
        BLACKLIST.add(Items.INFESTED_COBBLESTONE);
        BLACKLIST.add(Items.INFESTED_DEEPSLATE);
        BLACKLIST.add(Items.INFESTED_STONE_BRICKS);
        BLACKLIST.add(Items.INFESTED_MOSSY_STONE_BRICKS);
        BLACKLIST.add(Items.INFESTED_CRACKED_STONE_BRICKS);
        BLACKLIST.add(Items.INFESTED_CHISELED_STONE_BRICKS);
        BLACKLIST.add(Items.POTION);
        BLACKLIST.add(Items.SPLASH_POTION);
        BLACKLIST.add(Items.LINGERING_POTION);
        BLACKLIST.add(Items.TIPPED_ARROW);
        BLACKLIST.add(Items.ENCHANTED_BOOK);
        BLACKLIST.add(Items.WRITTEN_BOOK);
        BLACKLIST.add(Items.SUSPICIOUS_STEW);
        BLACKLIST.add(Items.FILLED_MAP);
    }
}
