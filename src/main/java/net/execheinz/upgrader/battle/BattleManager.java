package net.execheinz.upgrader.battle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.execheinz.upgrader.cases.CaseDefinition;
import net.execheinz.upgrader.cases.CaseLoot;
import net.execheinz.upgrader.economy.CaseStash;
import net.execheinz.upgrader.economy.PlayerBalance;
import net.execheinz.upgrader.network.ClientboundArenaLobbyPacket;
import net.execheinz.upgrader.network.ClientboundBattleStatePacket;
import net.execheinz.upgrader.network.ModNetwork;
import net.execheinz.upgrader.value.ItemValues;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * PvP battle cases: host creates invite → guest accepts → both open → higher value takes both.
 */
public final class BattleManager {
    public enum Phase {
        WAITING, OPENING, REVEAL, DONE
    }

    public static final int OPENING_TICKS = 120;
    public static final int REVEAL_TICKS = 100;

    private static BattleManager INSTANCE;

    private final MinecraftServer server;
    private final Map<UUID, Session> byHost = new ConcurrentHashMap<>();
    private final Map<UUID, Session> byPlayer = new ConcurrentHashMap<>();

    public static final class Session {
        public final UUID hostId;
        public final String hostName;
        public final String caseId;
        public final long casePrice;
        public UUID guestId;
        public String guestName = "";
        public Phase phase = Phase.WAITING;
        public int ticksLeft;
        public ItemStack hostLoot = ItemStack.EMPTY;
        public ItemStack guestLoot = ItemStack.EMPTY;
        public long hostValue;
        public long guestValue;
        /** 0 = host, 1 = guest, 2 = tie */
        public int winner = 2;
        public boolean settled;

        Session(UUID hostId, String hostName, String caseId, long casePrice) {
            this.hostId = hostId;
            this.hostName = hostName;
            this.caseId = caseId;
            this.casePrice = casePrice;
        }
    }

    private BattleManager(MinecraftServer server) {
        this.server = server;
    }

    public static void start(MinecraftServer server) {
        INSTANCE = new BattleManager(server);
    }

    public static void stop() {
        INSTANCE = null;
    }

    public static BattleManager get() {
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
                if (s.ticksLeft % 20 == 0) {
                    this.syncSession(s);
                }
                continue;
            }
            if (s.phase == Phase.OPENING) {
                s.phase = Phase.REVEAL;
                s.ticksLeft = REVEAL_TICKS;
                this.settle(s);
                this.syncSession(s);
                this.broadcastLobby();
            } else if (s.phase == Phase.REVEAL) {
                s.phase = Phase.DONE;
                this.clearSession(s);
                it.remove();
                this.broadcastLobby();
            }
        }
    }

    public boolean create(ServerPlayer host, String caseId) {
        if (this.byPlayer.containsKey(host.getUUID())) {
            return false;
        }
        CaseDefinition def = CaseDefinition.byId(caseId);
        if (def == null) {
            return false;
        }
        if (!PlayerBalance.trySpend(host, def.price())) {
            return false;
        }
        Session s = new Session(host.getUUID(), host.getGameProfile().getName(), caseId, def.price());
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
        PlayerBalance.add(host, s.casePrice);
        this.clearSession(s);
        this.byHost.remove(host.getUUID());
        this.broadcastLobby();
        ModNetwork.sendTo(host, ClientboundBattleStatePacket.idle());
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
        if (!PlayerBalance.trySpend(guest, s.casePrice)) {
            return false;
        }
        ServerPlayer host = this.server.getPlayerList().getPlayer(s.hostId);
        CaseDefinition def = CaseDefinition.byId(s.caseId);
        if (host == null || def == null) {
            PlayerBalance.add(guest, s.casePrice);
            return false;
        }
        s.guestId = guest.getUUID();
        s.guestName = guest.getGameProfile().getName();
        this.byPlayer.put(guest.getUUID(), s);
        s.hostLoot = CaseLoot.roll(host.level(), def, host.getRandom());
        s.guestLoot = CaseLoot.roll(guest.level(), def, guest.getRandom());
        s.hostValue = Math.max(0L, Math.round(ItemValues.stackValue(host.level(), s.hostLoot)));
        s.guestValue = Math.max(0L, Math.round(ItemValues.stackValue(guest.level(), s.guestLoot)));
        if (s.hostValue > s.guestValue) {
            s.winner = 0;
        } else if (s.guestValue > s.hostValue) {
            s.winner = 1;
        } else {
            s.winner = 2;
        }
        s.phase = Phase.OPENING;
        s.ticksLeft = OPENING_TICKS;
        this.syncSession(s);
        this.broadcastLobby();
        return true;
    }

    private void settle(Session s) {
        if (s.settled) {
            return;
        }
        s.settled = true;
        ServerPlayer host = this.server.getPlayerList().getPlayer(s.hostId);
        ServerPlayer guest = s.guestId == null ? null : this.server.getPlayerList().getPlayer(s.guestId);
        if (s.winner == 0 && host != null) {
            CaseStash.add(host, s.hostLoot.copy());
            CaseStash.add(host, s.guestLoot.copy());
        } else if (s.winner == 1 && guest != null) {
            CaseStash.add(guest, s.hostLoot.copy());
            CaseStash.add(guest, s.guestLoot.copy());
        } else {
            if (host != null) {
                CaseStash.add(host, s.hostLoot.copy());
            }
            if (guest != null) {
                CaseStash.add(guest, s.guestLoot.copy());
            }
        }
    }

    private void clearSession(Session s) {
        this.byPlayer.remove(s.hostId);
        if (s.guestId != null) {
            this.byPlayer.remove(s.guestId);
        }
        ServerPlayer host = this.server.getPlayerList().getPlayer(s.hostId);
        if (host != null) {
            ModNetwork.sendTo(host, ClientboundBattleStatePacket.idle());
        }
        if (s.guestId != null) {
            ServerPlayer guest = this.server.getPlayerList().getPlayer(s.guestId);
            if (guest != null) {
                ModNetwork.sendTo(guest, ClientboundBattleStatePacket.idle());
            }
        }
    }

    private void syncSession(Session s) {
        ClientboundBattleStatePacket packet = ClientboundBattleStatePacket.from(s);
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
            ModNetwork.sendTo(player, ClientboundBattleStatePacket.from(s));
        } else {
            ModNetwork.sendTo(player, ClientboundBattleStatePacket.idle());
        }
        ModNetwork.sendTo(player, this.lobbyPacket());
    }

    public void broadcastLobby() {
        ModNetwork.sendToAll(this.lobbyPacket());
    }

    public ClientboundArenaLobbyPacket lobbyPacket() {
        ArrayList<ClientboundArenaLobbyPacket.PlayerView> players = new ArrayList<>();
        for (ServerPlayer p : this.server.getPlayerList().getPlayers()) {
            players.add(new ClientboundArenaLobbyPacket.PlayerView(
                p.getUUID().toString(),
                p.getGameProfile().getName()
            ));
        }
        ArrayList<ClientboundArenaLobbyPacket.BattleInvite> battles = new ArrayList<>();
        for (Session s : this.byHost.values()) {
            if (s.phase == Phase.WAITING) {
                battles.add(new ClientboundArenaLobbyPacket.BattleInvite(
                    s.hostId.toString(),
                    s.hostName,
                    s.caseId,
                    s.casePrice
                ));
            }
        }
        ArrayList<ClientboundArenaLobbyPacket.DuelInvite> duels = new ArrayList<>();
        net.execheinz.upgrader.duel.DuelManager duelsMgr = net.execheinz.upgrader.duel.DuelManager.get();
        if (duelsMgr != null) {
            duels.addAll(duelsMgr.openInvites());
        }
        return new ClientboundArenaLobbyPacket(players, battles, duels);
    }

    public void onLogout(ServerPlayer player) {
        Session asHost = this.byHost.get(player.getUUID());
        if (asHost != null && asHost.phase == Phase.WAITING) {
            this.cancel(player);
            return;
        }
        Session s = this.byPlayer.get(player.getUUID());
        if (s != null && s.phase == Phase.WAITING) {
            // guest shouldn't be in WAITING in byPlayer without being host
            this.byPlayer.remove(player.getUUID());
        }
    }
}
