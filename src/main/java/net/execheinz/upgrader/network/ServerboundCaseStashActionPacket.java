package net.execheinz.upgrader.network;

import net.execheinz.upgrader.economy.CaseStash;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ServerboundCaseStashActionPacket(Action action, int slot) {
    public enum Action {
        WITHDRAW_ONE,
        WITHDRAW_ALL,
        SELL_ONE,
        SELL_ALL
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(this.action);
        buf.writeVarInt(this.slot);
    }

    public static ServerboundCaseStashActionPacket decode(FriendlyByteBuf buf) {
        return new ServerboundCaseStashActionPacket(buf.readEnum(Action.class), buf.readVarInt());
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            switch (this.action) {
                case WITHDRAW_ONE -> CaseStash.withdraw(player, this.slot);
                case WITHDRAW_ALL -> CaseStash.withdrawAll(player);
                case SELL_ONE -> CaseStash.sell(player, this.slot);
                case SELL_ALL -> CaseStash.sellAll(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
