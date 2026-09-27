package net.execheinz.upgrader.doublegame;

public enum DoubleColor {
    WHITE(2, 0xFFE8E8E8, 45),
    RED(3, 0xFFE74C3C, 45),
    GREEN(5, 0xFF2ECC71, 8),
    GOLD(10, 0xFFF1C40F, 2);

    private final int multiplier;
    private final int argb;
    private final int weight;

    DoubleColor(int multiplier, int argb, int weight) {
        this.multiplier = multiplier;
        this.argb = argb;
        this.weight = weight;
    }

    public int multiplier() {
        return this.multiplier;
    }

    public int argb() {
        return this.argb;
    }

    public int weight() {
        return this.weight;
    }

    public static int totalWeight() {
        int sum = 0;
        for (DoubleColor c : values()) {
            sum += c.weight;
        }
        return sum;
    }

    public static DoubleColor byOrdinalSafe(int ordinal) {
        DoubleColor[] all = values();
        if (ordinal < 0 || ordinal >= all.length) {
            return WHITE;
        }
        return all[ordinal];
    }
}
