package net.execheinz.upgrader.doublegame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.execheinz.upgrader.economy.PlayerBalance;
import net.execheinz.upgrader.network.ClientboundDoubleStatePacket;
import net.execheinz.upgrader.network.ModNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * Server-wide Double roulette.
 * Bets stay active if the player disconnects — payout is applied online or queued offline.
 * History is persisted and clipped to the visible UI line.
 */
public final class DoubleGame {
    public enum Phase {
        BETTING, SPINNING, RESULT
    }

    public static final int BETTING_TICKS = 300;
    public static final int SPINNING_TICKS = 120;
    public static final int RESULT_TICKS = 100;
    /** Visible chips on the history line (matches DoublePersist.HISTORY_LINE). */
    public static final int HISTORY_SIZE = DoublePersist.HISTORY_LINE;
    public static final long MIN_BET = 10L;

    private static DoubleGame INSTANCE;

    private final MinecraftServer server;
    private Phase phase = Phase.BETTING;
    private int ticksLeft = BETTING_TICKS;
    private long roundId = 1L;
    private DoubleColor lastResult = DoubleColor.WHITE;
    private final Map<UUID, Bet> bets = new HashMap<>();
    private final List<DoubleColor> history = new ArrayList<>();
    private final RandomSource random = RandomSource.create();

    public record Bet(UUID playerId, String playerName, DoubleColor color, long amount) {
    }

    private DoubleGame(MinecraftServer server) {
        this.server = server;
        DoublePersist persist = DoublePersist.get(server);
        for (int ordinal : persist.historyView()) {
            this.history.add(DoubleColor.byOrdinalSafe(ordinal));
        }
    }

    public static void start(MinecraftServer server) {
        INSTANCE = new DoubleGame(server);
    }

    public static void stop() {
        INSTANCE = null;
    }

    public static DoubleGame get() {
        return INSTANCE;
    }

    public void tick() {
        if (--this.ticksLeft > 0) {
            if (this.ticksLeft % 20 == 0) {
                this.broadcast();
            }
            return;
        }
        switch (this.phase) {
            case BETTING -> this.beginSpin();
            case SPINNING -> this.settle();
            case RESULT -> this.beginBetting();
        }
    }

    private void beginSpin() {
        this.lastResult = this.roll();
        this.phase = Phase.SPINNING;
        this.ticksLeft = SPINNING_TICKS;
        this.broadcast();
    }

    private void settle() {
        DoublePersist persist = DoublePersist.get(this.server);
        for (Bet bet : new ArrayList<>(this.bets.values())) {
            if (bet.color() != this.lastResult) {
                continue;
            }
            long win = bet.amount() * (long) bet.color().multiplier();
            ServerPlayer player = this.server.getPlayerList().getPlayer(bet.playerId());
            if (player != null) {
                PlayerBalance.add(player, win);
            } else {
                // Player left — keep the win until they rejoin.
                persist.addPending(bet.playerId(), win);
            }
        }
        this.history.add(0, this.lastResult);
        while (this.history.size() > HISTORY_SIZE) {
            this.history.remove(this.history.size() - 1);
        }
        persist.pushHistory(this.lastResult.ordinal());
        this.phase = Phase.RESULT;
        this.ticksLeft = RESULT_TICKS;
        this.broadcast();
    }

    private void beginBetting() {
        this.bets.clear();
        this.roundId++;
        this.phase = Phase.BETTING;
        this.ticksLeft = BETTING_TICKS;
        this.broadcast();
    }

    private DoubleColor roll() {
        int total = DoubleColor.totalWeight();
        int roll = this.random.nextInt(total);
        int cursor = 0;
        for (DoubleColor c : DoubleColor.values()) {
            cursor += c.weight();
            if (roll < cursor) {
                return c;
            }
        }
        return DoubleColor.WHITE;
    }

    public boolean placeBet(ServerPlayer player, DoubleColor color, long amount) {
        if (this.phase != Phase.BETTING) {
            return false;
        }
        if (amount < MIN_BET) {
            return false;
        }
        if (this.bets.containsKey(player.getUUID())) {
            return false;
        }
        if (!PlayerBalance.trySpend(player, amount)) {
            return false;
        }
        this.bets.put(player.getUUID(), new Bet(player.getUUID(), player.getGameProfile().getName(), color, amount));
        this.broadcast();
        return true;
    }

    public boolean hasBet(UUID playerId) {
        return this.bets.containsKey(playerId);
    }

    public void syncTo(ServerPlayer player) {
        ModNetwork.sendTo(player, this.toPacket());
    }

    private void broadcast() {
        ModNetwork.sendToAll(this.toPacket());
    }

    private ClientboundDoubleStatePacket toPacket() {
        List<ClientboundDoubleStatePacket.BetView> views = new ArrayList<>(this.bets.size());
        for (Bet bet : this.bets.values()) {
            views.add(new ClientboundDoubleStatePacket.BetView(bet.playerName(), bet.color().ordinal(), bet.amount()));
        }
        List<Integer> hist = new ArrayList<>(this.history.size());
        for (DoubleColor c : this.history) {
            hist.add(c.ordinal());
        }
        return new ClientboundDoubleStatePacket(
            this.roundId,
            this.phase.ordinal(),
            this.ticksLeft,
            this.lastResult.ordinal(),
            views,
            hist
        );
    }
}
