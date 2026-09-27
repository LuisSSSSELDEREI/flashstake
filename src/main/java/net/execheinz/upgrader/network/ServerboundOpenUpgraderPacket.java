package net.execheinz.upgrader.network;

import net.execheinz.upgrader.economy.PlayerBalance;
import net.execheinz.upgrader.menu.UpgraderMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ServerboundOpenUpgraderPacket() {
    public void encode(FriendlyByteBuf buf) {
    }

    public static ServerboundOpenUpgraderPacket decode(FriendlyByteBuf buf) {
        return new ServerboundOpenUpgraderPacket();
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new UpgraderMenu(id, inv),
                Component.translatable("menu.flashstake.title")
            ));
            PlayerBalance.sync(player);
        });
        ctx.setPacketHandled(true);
    }
}
