package net.execheinz.upgrader.network;

import java.util.function.Supplier;
import net.execheinz.upgrader.economy.CaseStash;
import net.execheinz.upgrader.economy.PlayerBalance;
import net.execheinz.upgrader.menu.CasesMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

public record ServerboundOpenCasesPacket() {
    public void encode(FriendlyByteBuf buf) {
    }

    public static ServerboundOpenCasesPacket decode(FriendlyByteBuf buf) {
        return new ServerboundOpenCasesPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inv, p) -> new CasesMenu(id, inv),
                Component.translatable("menu.flashstake.cases")
            ));
            PlayerBalance.sync(player);
            CaseStash.sync(player);
            net.execheinz.upgrader.economy.CasePending.sync(player);
        });
        ctx.setPacketHandled(true);
    }
}
