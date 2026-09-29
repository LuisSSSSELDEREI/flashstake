package net.execheinz.upgrader.doublegame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.execheinz.upgrader.economy.PlayerBalance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/** Offline payouts + colour history trimmed to the visible history line. */
public final class DoublePersist extends SavedData {
    private static final String DATA_NAME = "flashstake_double";
    public static final int HISTORY_LINE = 22;
    private static final SavedData.Factory<DoublePersist> FACTORY =
        new SavedData.Factory<>(DoublePersist::new, DoublePersist::load, DataFixTypes.LEVEL);

    private final Map<UUID, Long> pending = new HashMap<>();
    private final ArrayList<Integer> history = new ArrayList<>();

    public static DoublePersist get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public DoublePersist() {
    }

    public static DoublePersist load(CompoundTag tag) {
        DoublePersist data = new DoublePersist();
        ListTag payouts = tag.getList("pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < payouts.size(); ++i) {
            CompoundTag e = payouts.getCompound(i);
            try {
                data.pending.put(UUID.fromString(e.getString("id")), e.getLong("amt"));
            } catch (IllegalArgumentException ignored) {
            }
        }
        int[] hist = tag.getIntArray("history");
        for (int v : hist) {
            data.history.add(v);
        }
        while (data.history.size() > HISTORY_LINE) {
            data.history.remove(data.history.size() - 1);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag payouts = new ListTag();
        for (Map.Entry<UUID, Long> e : this.pending.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0L) {
                continue;
            }
            CompoundTag row = new CompoundTag();
            row.putString("id", e.getKey().toString());
            row.putLong("amt", e.getValue());
            payouts.add(row);
        }
        tag.put("pending", payouts);
        int n = Math.min(this.history.size(), HISTORY_LINE);
        int[] hist = new int[n];
        for (int i = 0; i < n; ++i) {
            hist[i] = this.history.get(i);
        }
        tag.putIntArray("history", hist);
        return tag;
    }

    public void addPending(UUID id, long amount) {
        if (amount <= 0L) {
            return;
        }
        this.pending.merge(id, amount, Long::sum);
        this.setDirty();
    }

    public void claim(ServerPlayer player) {
        Long amt = this.pending.remove(player.getUUID());
        if (amt != null && amt > 0L) {
            PlayerBalance.add(player, amt);
            this.setDirty();
        }
    }

    public List<Integer> historyView() {
        return List.copyOf(this.history);
    }

    public void pushHistory(int colorOrdinal) {
        this.history.add(0, colorOrdinal);
        while (this.history.size() > HISTORY_LINE) {
            this.history.remove(this.history.size() - 1);
        }
        this.setDirty();
    }
}
