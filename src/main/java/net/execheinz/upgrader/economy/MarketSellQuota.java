package net.execheinz.upgrader.economy;

import net.execheinz.upgrader.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fixed window quota: every {@link Config#marketSellWindowMinutes} the remaining
 * sell units reset exactly to {@link Config#marketSellLimit} (default 100 / 10 min).
 */
public final class MarketSellQuota {
    private static final String KEY = "FlashStakeMarketSellQuota";
    private static final String REMAINING = "remaining";
    private static final String WINDOW_START = "windowStart";

    private MarketSellQuota() {
    }

    public static int remaining(ServerPlayer player) {
        return ensure(player).getInt(REMAINING);
    }

    public static long windowMsLeft(ServerPlayer player) {
        CompoundTag tag = ensure(player);
        long start = tag.getLong(WINDOW_START);
        return Math.max(0L, start + windowMs() - System.currentTimeMillis());
    }

    /** Consume {@code amount} sell units. Returns false if over remaining quota. */
    public static boolean tryConsume(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return true;
        }
        CompoundTag tag = ensure(player);
        int left = tag.getInt(REMAINING);
        if (amount > left) {
            return false;
        }
        tag.putInt(REMAINING, left - amount);
        player.getPersistentData().put(KEY, tag);
        return true;
    }

    private static CompoundTag ensure(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.contains(KEY) ? root.getCompound(KEY) : new CompoundTag();
        long now = System.currentTimeMillis();
        long window = windowMs();
        boolean dirty = false;

        if (!tag.contains(WINDOW_START) || !tag.contains(REMAINING)) {
            tag.putLong(WINDOW_START, now);
            tag.putInt(REMAINING, Config.marketSellLimit);
            dirty = true;
        } else {
            long start = tag.getLong(WINDOW_START);
            if (start <= 0L) {
                tag.putLong(WINDOW_START, now);
                tag.putInt(REMAINING, Config.marketSellLimit);
                dirty = true;
            } else if (now >= start + window) {
                long periods = Math.max(1L, (now - start) / window);
                tag.putLong(WINDOW_START, start + periods * window);
                // Full reset to the configured limit every window — not a rolling top-up.
                tag.putInt(REMAINING, Config.marketSellLimit);
                dirty = true;
            }
        }

        // Clamp remaining if config limit changed upward/downward mid-window.
        int rem = tag.getInt(REMAINING);
        int limit = Config.marketSellLimit;
        if (rem > limit) {
            tag.putInt(REMAINING, limit);
            dirty = true;
        }
        if (rem < 0) {
            tag.putInt(REMAINING, 0);
            dirty = true;
        }

        if (dirty || !root.contains(KEY)) {
            root.put(KEY, tag);
        }
        return tag;
    }

    private static long windowMs() {
        return Math.max(1L, Config.marketSellWindowMinutes) * 60_000L;
    }
}
