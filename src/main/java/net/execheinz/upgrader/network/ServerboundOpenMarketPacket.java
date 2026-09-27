package net.execheinz.upgrader.network;

import net.execheinz.upgrader.economy.PlayerBalance;
import net.execheinz.upgrader.economy.MarketStock;
import net.execheinz.upgrader.menu.MarketMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ServerboundOpenMarketPacket() {
    public void encode(FriendlyByteBuf buf) {
    }

    public static ServerboundOpenMarketPacket decode(FriendlyByteBuf buf) {
        return new ServerboundOpenMarketPacket();
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new MarketMenu(id, inv),
                Component.translatable("menu.flashstake.market")
            ));
            PlayerBalance.sync(player);
            MarketStock stock = MarketStock.get(player.server);
            stock.ensureReady(player.server);
            stock.syncTo(player);
        });
        ctx.setPacketHandled(true);
    }
}
