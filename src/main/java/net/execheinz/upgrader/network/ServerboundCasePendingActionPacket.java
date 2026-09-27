package net.execheinz.upgrader.network;

import java.util.function.Supplier;
import net.execheinz.upgrader.economy.CasePending;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

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

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
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
