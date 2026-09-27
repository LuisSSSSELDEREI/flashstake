package net.execheinz.upgrader.network;

import net.execheinz.upgrader.economy.CasePending;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ServerboundCasePendingActionPacket(Action action) {
    public enum Action {
        KEEP,
        SELL
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(this.action);
    }

    public static ServerboundCasePendingActionPacket decode(FriendlyByteBuf buf) {
        return new ServerboundCasePendingActionPacket(buf.readEnum(Action.class));
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            if (this.action == Action.KEEP) {
                CasePending.keep(player);
            } else {
                CasePending.sell(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
