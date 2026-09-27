package net.execheinz.upgrader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.execheinz.upgrader.value.ItemValues;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = "flashstake", bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.DoubleValue HOUSE_EDGE = BUILDER.comment("Multiplier applied to the raw value ratio. 1.0 = fair odds, 0.9 = 10% house edge.").defineInRange("houseEdge", 0.9, 0.01, 1.0);
    private static final ForgeConfigSpec.DoubleValue MIN_CHANCE = BUILDER.comment("Lowest possible success chance (0.001 = 0.1%).").defineInRange("minChance", 0.001, 0.0, 1.0);
    private static final ForgeConfigSpec.DoubleValue MAX_CHANCE = BUILDER.comment("Highest possible success chance. Keeps 'downgrades' from being a free win.").defineInRange("maxChance", 0.9, 0.01, 1.0);
    private static final ForgeConfigSpec.IntValue SPIN_TICKS = BUILDER.comment("How long the wheel spins before the result is applied, in ticks (20 = 1 second).").defineInRange("spinDurationTicks", 70, 20, 600);
    private static final ForgeConfigSpec.DoubleValue DEFAULT_PREFERRED_CHANCE = BUILDER.comment("Default preferred upgrade chance shown in the UI (0.10 = 10%).").defineInRange("defaultPreferredChance", 0.10, 0.01, 0.9);
    private static final ForgeConfigSpec.DoubleValue MARKET_SELL_RATE = BUILDER.comment("Fraction of item value paid when selling on the market.").defineInRange("marketSellRate", 1.0, 0.01, 2.0);
    private static final ForgeConfigSpec.DoubleValue MARKET_BUY_RATE = BUILDER.comment("Multiplier on item value when buying from the market.").defineInRange("marketBuyRate", 1.0, 0.01, 5.0);
    private static final ForgeConfigSpec.IntValue MARKET_SELL_LIMIT = BUILDER.comment("Max item units a player can sell within the sell window.").defineInRange("marketSellLimit", 100, 1, 10000);
    private static final ForgeConfigSpec.IntValue MARKET_SELL_WINDOW = BUILDER.comment("Sell quota window in minutes.").defineInRange("marketSellWindowMinutes", 10, 1, 1440);
    private static final ForgeConfigSpec.IntValue MARKET_STOCK_REFRESH = BUILDER.comment("How often server-wide buy stock refreshes, in minutes.").defineInRange("marketStockRefreshMinutes", 15, 1, 10080);
    private static final ForgeConfigSpec.IntValue MARKET_STOCK_MIN = BUILDER.comment("Minimum random stock per item on refresh.").defineInRange("marketStockMin", 0, 0, 10000);
    private static final ForgeConfigSpec.IntValue MARKET_STOCK_MAX = BUILDER.comment("Maximum random stock per item on refresh.").defineInRange("marketStockMax", 60, 0, 10000);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_OVERRIDES = BUILDER.comment("Hard-coded item values, format 'modid:item=value'.").defineListAllowEmpty("valueOverrides", List.of(), o -> o instanceof String s && s.contains("="));
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLIST = BUILDER.comment("Items that can never be used as an input or picked as a target.").defineListAllowEmpty("blacklist", List.of(), o -> o instanceof String);
    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static double houseEdge = 0.9;
    public static double minChance = 0.001;
    public static double maxChance = 0.9;
    public static int spinTicks = 70;
    public static double defaultPreferredChance = 0.10;
    public static double marketSellRate = 1.0;
    public static double marketBuyRate = 1.0;
    public static int marketSellLimit = 100;
    public static int marketSellWindowMinutes = 10;
    public static int marketStockRefreshMinutes = 15;
    public static int marketStockMin = 0;
    public static int marketStockMax = 60;
    public static Map<String, Double> valueOverrides = Map.of();
    public static Set<String> blacklist = Set.of();

    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        houseEdge = HOUSE_EDGE.get();
        minChance = MIN_CHANCE.get();
        maxChance = Math.max(MAX_CHANCE.get(), MIN_CHANCE.get());
        spinTicks = SPIN_TICKS.get();
        defaultPreferredChance = DEFAULT_PREFERRED_CHANCE.get();
        marketSellRate = MARKET_SELL_RATE.get();
        marketBuyRate = MARKET_BUY_RATE.get();
        marketSellLimit = MARKET_SELL_LIMIT.get();
        marketSellWindowMinutes = MARKET_SELL_WINDOW.get();
        marketStockRefreshMinutes = MARKET_STOCK_REFRESH.get();
        marketStockMin = MARKET_STOCK_MIN.get();
        marketStockMax = MARKET_STOCK_MAX.get();
        HashMap<String, Double> overrides = new HashMap<>();
        for (String entry : VALUE_OVERRIDES.get()) {
            int split = entry.indexOf('=');
            if (split <= 0) {
                continue;
            }
            try {
                overrides.put(entry.substring(0, split).trim(), Double.parseDouble(entry.substring(split + 1).trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        valueOverrides = Map.copyOf(overrides);
        blacklist = Set.copyOf(new HashSet<>(BLACKLIST.get()));
        ItemValues.invalidate();
    }
}
