package net.execheinz.upgrader.duel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.execheinz.upgrader.economy.PlayerBalance;
import net.execheinz.upgrader.network.ClientboundArenaLobbyPacket;
import net.execheinz.upgrader.network.ClientboundDuelStatePacket;
import net.execheinz.upgrader.network.ModNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * PvP coin duels: host escrows stake → guest matches → 50/50 roll → winner takes pot (3% house).
 */
public final class DuelManager {
    public enum Phase {
        WAITING, SPINNING, RESULT, DONE
    }

    public static final long MIN_STAKE = 10L;
    public static final int SPIN_TICKS = 100;
    public static final int RESULT_TICKS = 80;
    private static final double HOUSE = 0.03;

    private static DuelManager INSTANCE;

    private final MinecraftServer server;
    private final Map<UUID, Session> byHost = new ConcurrentHashMap<>();
    private final Map<UUID, Session> byPlayer = new ConcurrentHashMap<>();

    public static final class Session {
        public final UUID hostId;
        public final String hostName;
        public final long stake;
        public UUID guestId;
        public String guestName = "";
        public Phase phase = Phase.WAITING;
        public int ticksLeft;
        /** 0 = host, 1 = guest */
        public int winner;
        public long payout;
        public boolean settled;

        Session(UUID hostId, String hostName, long stake) {
            this.hostId = hostId;
            this.hostName = hostName;
            this.stake = stake;
        }
    }

    private DuelManager(MinecraftServer server) {
        this.server = server;
    }

    public static void start(MinecraftServer server) {
        INSTANCE = new DuelManager(server);
    }

    public static void stop() {
        INSTANCE = null;
    }

    public static DuelManager get() {
        return INSTANCE;
    }

    public void tick() {
        Iterator<Map.Entry<UUID, Session>> it = this.byHost.entrySet().iterator();
        while (it.hasNext()) {
            Session s = it.next().getValue();
            if (s.phase == Phase.WAITING) {
                continue;
            }
            if (--s.ticksLeft > 0) {
                if (s.ticksLeft % 10 == 0) {
                    this.syncSession(s);
                }
                continue;
            }
            if (s.phase == Phase.SPINNING) {
                this.settle(s);
                s.phase = Phase.RESULT;
                s.ticksLeft = RESULT_TICKS;
                this.syncSession(s);
                this.broadcastLobby();
            } else if (s.phase == Phase.RESULT) {
                s.phase = Phase.DONE;
                this.clearSession(s);
                it.remove();
                this.broadcastLobby();
            }
        }
    }

    public boolean create(ServerPlayer host, long stake) {
        if (stake < MIN_STAKE || this.byPlayer.containsKey(host.getUUID())) {
            return false;
        }
        if (!PlayerBalance.trySpend(host, stake)) {
            return false;
        }
        Session s = new Session(host.getUUID(), host.getGameProfile().getName(), stake);
        this.byHost.put(host.getUUID(), s);
        this.byPlayer.put(host.getUUID(), s);
        this.syncSession(s);
        this.broadcastLobby();
        return true;
    }

    public boolean cancel(ServerPlayer host) {
        Session s = this.byHost.get(host.getUUID());
        if (s == null || s.phase != Phase.WAITING || !s.hostId.equals(host.getUUID())) {
            return false;
        }
        PlayerBalance.add(host, s.stake);
        this.clearSession(s);
        this.byHost.remove(host.getUUID());
        this.broadcastLobby();
        ModNetwork.sendTo(host, ClientboundDuelStatePacket.idle());
        return true;
    }

    public boolean accept(ServerPlayer guest, UUID hostId) {
        if (this.byPlayer.containsKey(guest.getUUID())) {
            return false;
        }
        Session s = this.byHost.get(hostId);
        if (s == null || s.phase != Phase.WAITING || s.guestId != null) {
            return false;
        }
        if (guest.getUUID().equals(s.hostId)) {
            return false;
        }
        if (!PlayerBalance.trySpend(guest, s.stake)) {
            return false;
        }
        s.guestId = guest.getUUID();
        s.guestName = guest.getGameProfile().getName();
        this.byPlayer.put(guest.getUUID(), s);
        s.winner = guest.getRandom().nextBoolean() ? 0 : 1;
        long pot = Math.round(s.stake * 2L * (1.0 - HOUSE));
        s.payout = Math.max(s.stake, pot);
        s.phase = Phase.SPINNING;
        s.ticksLeft = SPIN_TICKS;
        this.syncSession(s);
        this.broadcastLobby();
        return true;
    }

    private void settle(Session s) {
        if (s.settled) {
            return;
        }
        s.settled = true;
        UUID winId = s.winner == 0 ? s.hostId : s.guestId;
        ServerPlayer winner = winId == null ? null : this.server.getPlayerList().getPlayer(winId);
        if (winner != null) {
            PlayerBalance.add(winner, s.payout);
        }
    }

    private void clearSession(Session s) {
        this.byPlayer.remove(s.hostId);
        if (s.guestId != null) {
            this.byPlayer.remove(s.guestId);
        }
        ServerPlayer host = this.server.getPlayerList().getPlayer(s.hostId);
        if (host != null) {
            ModNetwork.sendTo(host, ClientboundDuelStatePacket.idle());
        }
        if (s.guestId != null) {
            ServerPlayer guest = this.server.getPlayerList().getPlayer(s.guestId);
            if (guest != null) {
                ModNetwork.sendTo(guest, ClientboundDuelStatePacket.idle());
            }
        }
    }

    private void syncSession(Session s) {
        ClientboundDuelStatePacket packet = ClientboundDuelStatePacket.from(s);
        ServerPlayer host = this.server.getPlayerList().getPlayer(s.hostId);
        if (host != null) {
            ModNetwork.sendTo(host, packet);
        }
        if (s.guestId != null) {
            ServerPlayer guest = this.server.getPlayerList().getPlayer(s.guestId);
            if (guest != null) {
                ModNetwork.sendTo(guest, packet);
            }
        }
    }

    public void syncTo(ServerPlayer player) {
        Session s = this.byPlayer.get(player.getUUID());
        if (s != null) {
            ModNetwork.sendTo(player, ClientboundDuelStatePacket.from(s));
        } else {
            ModNetwork.sendTo(player, ClientboundDuelStatePacket.idle());
        }
    }

    public List<ClientboundArenaLobbyPacket.DuelInvite> openInvites() {
        ArrayList<ClientboundArenaLobbyPacket.DuelInvite> list = new ArrayList<>();
        for (Session s : this.byHost.values()) {
            if (s.phase == Phase.WAITING) {
                list.add(new ClientboundArenaLobbyPacket.DuelInvite(
                    s.hostId.toString(),
                    s.hostName,
                    s.stake
                ));
            }
        }
        return list;
    }

    private void broadcastLobby() {
        net.execheinz.upgrader.battle.BattleManager battles = net.execheinz.upgrader.battle.BattleManager.get();
        if (battles != null) {
            battles.broadcastLobby();
        }
    }

    public void onLogout(ServerPlayer player) {
        Session asHost = this.byHost.get(player.getUUID());
        if (asHost != null && asHost.phase == Phase.WAITING) {
            this.cancel(player);
        }
    }
}
