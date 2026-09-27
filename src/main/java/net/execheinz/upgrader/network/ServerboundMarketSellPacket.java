package net.execheinz.upgrader.network;

import net.execheinz.upgrader.Config;
import net.execheinz.upgrader.economy.MarketSellQuota;
import net.execheinz.upgrader.economy.MarketStock;
import net.execheinz.upgrader.economy.PlayerBalance;
import net.execheinz.upgrader.menu.MarketMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Confirm sale of everything currently in the market sell tray. */
public record ServerboundMarketSellPacket() {
    public void encode(FriendlyByteBuf buf) {
    }

    public static ServerboundMarketSellPacket decode(FriendlyByteBuf buf) {
        return new ServerboundMarketSellPacket();
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || !(player.containerMenu instanceof MarketMenu menu)) {
                return;
            }
            int units = menu.sellUnitCount();
            if (units <= 0 || !menu.hasSellableItems()) {
                return;
            }
            int left = MarketSellQuota.remaining(player);
            if (units > left) {
                player.sendSystemMessage(Component.translatable("gui.flashstake.market.sell_limit", left, Config.marketSellLimit));
                MarketStock.get(player.server).syncTo(player);
                return;
            }
            long value = menu.sellTotalValue(player.level());
            if (value <= 0L) {
                return;
            }
            if (!MarketSellQuota.tryConsume(player, units)) {
                left = MarketSellQuota.remaining(player);
                player.sendSystemMessage(Component.translatable("gui.flashstake.market.sell_limit", left, Config.marketSellLimit));
                MarketStock.get(player.server).syncTo(player);
                return;
            }
            menu.clearSellTray();
            PlayerBalance.add(player, value);
            MarketStock.get(player.server).syncTo(player);
        });
        ctx.setPacketHandled(true);
    }
}
