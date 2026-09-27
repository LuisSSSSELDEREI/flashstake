package net.execheinz.upgrader.economy;

import net.execheinz.upgrader.network.ClientboundBalancePacket;
import net.execheinz.upgrader.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class PlayerBalance {
    private static final String KEY = "FlashStakeCoins";

    private PlayerBalance() {
    }

    public static long get(Player player) {
        return player.getPersistentData().getLong(KEY);
    }

    public static void set(ServerPlayer player, long amount) {
        long clamped = Math.max(0L, amount);
        CompoundTag data = player.getPersistentData();
        data.putLong(KEY, clamped);
        ModNetwork.sendTo(player, new ClientboundBalancePacket(clamped));
    }

    public static void add(ServerPlayer player, long delta) {
        if (delta == 0L) {
            return;
        }
        set(player, get(player) + delta);
    }

    public static boolean trySpend(ServerPlayer player, long cost) {
        if (cost <= 0L) {
            return true;
        }
        long balance = get(player);
        if (balance < cost) {
            return false;
        }
        set(player, balance - cost);
        return true;
    }

    public static void sync(ServerPlayer player) {
        ModNetwork.sendTo(player, new ClientboundBalancePacket(get(player)));
    }
}
