package net.execheinz.upgrader.value;

import net.execheinz.upgrader.Config;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class UpgradeOdds {
    public static double chance(Level level, ItemStack input, Item target, int targetCount) {
        if (input.isEmpty() || target == null) {
            return 0.0;
        }
        double in = ItemValues.stackValue(level, input);
        double out = ItemValues.unitValue(level, target) * (double) Math.max(1, targetCount);
        return chance(in, out);
    }

    public static double chance(double inputValue, double targetValue) {
        if (inputValue <= 0.0 || targetValue <= 0.0) {
            return 0.0;
        }
        double raw = Config.houseEdge * inputValue / targetValue;
        return Math.max(Config.minChance, Math.min(Config.maxChance, raw));
    }

    private UpgradeOdds() {
    }
}
