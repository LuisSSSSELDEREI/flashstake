package net.execheinz.upgrader;

import net.execheinz.upgrader.battle.BattleManager;
import net.execheinz.upgrader.doublegame.DoubleGame;
import net.execheinz.upgrader.doublegame.DoublePersist;
import net.execheinz.upgrader.duel.DuelManager;
import net.execheinz.upgrader.economy.MarketStock;
import net.execheinz.upgrader.economy.PlayerBalance;
import java.util.List;
import java.util.Map;
import net.execheinz.upgrader.network.ClientboundPriceTablePacket;
import net.execheinz.upgrader.network.ModNetwork;
import net.execheinz.upgrader.value.ItemValues;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "flashstake", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ModEvents {
    private static int stockTick;

    @SubscribeEvent
    static void onReload(AddReloadListenerEvent event) {
        ItemValues.invalidate();
    }

    /** Fires on join (single player) and after /reload (all players) — push server prices. */
    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        List<ServerPlayer> targets = event.getPlayer() != null ? List.of(event.getPlayer()) : List.copyOf(event.getPlayerList().getPlayers());
        if (targets.isEmpty()) {
            return;
        }
        ServerLevel level = event.getPlayerList().getServer().overworld();
        Map<Item, Double> table = ItemValues.snapshot(level);
        if (table == null || table.isEmpty()) {
            return;
        }
        ClientboundPriceTablePacket packet = new ClientboundPriceTablePacket(table);
        for (ServerPlayer player : targets) {
            ModNetwork.sendTo(player, packet);
        }
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerBalance.sync(player);
            MarketStock.get(player.server).ensureReady(player.server);
            MarketStock.get(player.server).syncTo(player);
            DoublePersist.get(player.server).claim(player);
            DoubleGame game = DoubleGame.get();
            if (game != null) {
                game.syncTo(player);
            }
            BattleManager battles = BattleManager.get();
            if (battles != null) {
                battles.broadcastLobby();
            }
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BattleManager battles = BattleManager.get();
            if (battles != null) {
                battles.onLogout(player);
            }
            DuelManager duels = DuelManager.get();
            if (duels != null) {
                duels.onLogout(player);
            }
        }
    }

    @SubscribeEvent
    static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        long balance = PlayerBalance.get(event.getOriginal());
        PlayerBalance.set(player, balance);
    }

    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        DoubleGame.start(event.getServer());
        BattleManager.start(event.getServer());
        DuelManager.start(event.getServer());
        MarketStock.get(event.getServer()).ensureReady(event.getServer());
    }

    @SubscribeEvent
    static void onServerStopping(ServerStoppingEvent event) {
        DoubleGame.stop();
        BattleManager.stop();
        DuelManager.stop();
    }

    @SubscribeEvent
    static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        DoubleGame game = DoubleGame.get();
        if (game != null) {
            game.tick();
        }
        BattleManager battles = BattleManager.get();
        if (battles != null) {
            battles.tick();
        }
        DuelManager duels = DuelManager.get();
        if (duels != null) {
            duels.tick();
        }
        if (++stockTick >= 20) {
            stockTick = 0;
            MarketStock.get(event.getServer()).tick(event.getServer());
        }
    }

    private ModEvents() {
    }
}
