package net.execheinz.upgrader.network;

import net.execheinz.upgrader.battle.BattleManager;
import net.execheinz.upgrader.duel.DuelManager;
import net.execheinz.upgrader.economy.CaseStash;
import net.execheinz.upgrader.economy.PlayerBalance;
import net.execheinz.upgrader.menu.ArenaMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ServerboundOpenArenaPacket() {
    public void encode(FriendlyByteBuf buf) {
    }

    public static ServerboundOpenArenaPacket decode(FriendlyByteBuf buf) {
        return new ServerboundOpenArenaPacket();
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ArenaMenu(id, inv),
                Component.translatable("menu.flashstake.arena")
            ));
            PlayerBalance.sync(player);
            CaseStash.sync(player);
            BattleManager battles = BattleManager.get();
            if (battles != null) {
                battles.syncTo(player);
            }
            DuelManager duels = DuelManager.get();
            if (duels != null) {
                duels.syncTo(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
