package net.execheinz.upgrader.network;

import net.execheinz.upgrader.doublegame.DoubleGame;
import net.execheinz.upgrader.economy.PlayerBalance;
import net.execheinz.upgrader.menu.DoubleMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ServerboundOpenDoublePacket() {
    public void encode(FriendlyByteBuf buf) {
    }

    public static ServerboundOpenDoublePacket decode(FriendlyByteBuf buf) {
        return new ServerboundOpenDoublePacket();
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new DoubleMenu(id, inv),
                Component.translatable("menu.flashstake.double")
            ));
            PlayerBalance.sync(player);
            DoubleGame game = DoubleGame.get();
            if (game != null) {
                game.syncTo(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
