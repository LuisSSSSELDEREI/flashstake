package net.execheinz.upgrader;

import net.execheinz.upgrader.doublegame.DoubleGame;
import net.execheinz.upgrader.economy.MarketStock;
import net.execheinz.upgrader.economy.PlayerBalance;
import net.execheinz.upgrader.value.ItemValues;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
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

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerBalance.sync(player);
            MarketStock.get(player.server).ensureReady(player.server);
            MarketStock.get(player.server).syncTo(player);
            DoubleGame game = DoubleGame.get();
            if (game != null) {
                game.syncTo(player);
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
        MarketStock.get(event.getServer()).ensureReady(event.getServer());
    }

    @SubscribeEvent
    static void onServerStopping(ServerStoppingEvent event) {
        DoubleGame.stop();
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
        if (++stockTick >= 20) {
            stockTick = 0;
            MarketStock.get(event.getServer()).tick(event.getServer());
        }
    }

    private ModEvents() {
    }
}
