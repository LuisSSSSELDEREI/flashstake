package net.execheinz.upgrader.economy;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.execheinz.upgrader.Config;
import net.execheinz.upgrader.network.ClientboundMarketStatePacket;
import net.execheinz.upgrader.network.ModNetwork;
import net.execheinz.upgrader.value.ItemValues;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Server-wide market buy stock. Restocks every hour with a random amount per item.
 */
public final class MarketStock extends SavedData {
    private static final String DATA_NAME = "flashstake_market_stock";

    private final Map<String, Integer> stock = new HashMap<>();
    private long nextRefreshMs;
    private final Random random = new Random();

    public MarketStock() {
        this.nextRefreshMs = System.currentTimeMillis();
    }

    public static MarketStock get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(MarketStock::load, MarketStock::new, DATA_NAME);
    }

    public static MarketStock load(CompoundTag tag) {
        MarketStock data = new MarketStock();
        data.nextRefreshMs = tag.getLong("NextRefresh");
        CompoundTag stocks = tag.getCompound("Stock");
        for (String key : stocks.getAllKeys()) {
            data.stock.put(key, Math.max(0, stocks.getInt(key)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("NextRefresh", this.nextRefreshMs);
        CompoundTag stocks = new CompoundTag();
        for (Map.Entry<String, Integer> e : this.stock.entrySet()) {
            if (e.getValue() > 0) {
                stocks.putInt(e.getKey(), e.getValue());
            }
        }
        tag.put("Stock", stocks);
        return tag;
    }

    public void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        if (now < this.nextRefreshMs) {
            return;
        }
        this.restock(server);
        this.nextRefreshMs = now + refreshMs();
        this.setDirty();
        this.syncAll(server);
    }

    public void ensureReady(MinecraftServer server) {
        if (this.stock.isEmpty() || System.currentTimeMillis() >= this.nextRefreshMs) {
            this.restock(server);
            this.nextRefreshMs = System.currentTimeMillis() + refreshMs();
            this.setDirty();
        }
    }

    private void restock(MinecraftServer server) {
        this.stock.clear();
        int min = Math.min(Config.marketStockMin, Config.marketStockMax);
        int max = Math.max(Config.marketStockMin, Config.marketStockMax);
        for (Item item : ItemValues.catalog(server.overworld())) {
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
            if (key == null) {
                continue;
            }
            int qty = min >= max ? min : min + this.random.nextInt(max - min + 1);
            if (qty > 0) {
                this.stock.put(key.toString(), qty);
            }
        }
    }

    public int available(Item item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        if (key == null) {
            return 0;
        }
        return this.stock.getOrDefault(key.toString(), 0);
    }

    public boolean tryTake(Item item, int amount) {
        if (amount <= 0) {
            return true;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        if (key == null) {
            return false;
        }
        String id = key.toString();
        int have = this.stock.getOrDefault(id, 0);
        if (have < amount) {
            return false;
        }
        int left = have - amount;
        if (left <= 0) {
            this.stock.remove(id);
        } else {
            this.stock.put(id, left);
        }
        this.setDirty();
        return true;
    }

    public Map<String, Integer> snapshot() {
        return Map.copyOf(this.stock);
    }

    public long refreshInMs() {
        return Math.max(0L, this.nextRefreshMs - System.currentTimeMillis());
    }

    public void syncTo(ServerPlayer player) {
        ModNetwork.sendTo(player, new ClientboundMarketStatePacket(
            this.snapshot(),
            this.refreshInMs(),
            MarketSellQuota.remaining(player),
            MarketSellQuota.windowMsLeft(player)
        ));
    }

    public void syncAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            this.syncTo(player);
        }
    }

    private static long refreshMs() {
        return Math.max(1L, Config.marketStockRefreshMinutes) * 60_000L;
    }
}
