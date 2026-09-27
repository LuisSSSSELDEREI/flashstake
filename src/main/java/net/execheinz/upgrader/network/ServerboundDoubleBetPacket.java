package net.execheinz.upgrader.network;

import net.execheinz.upgrader.doublegame.DoubleColor;
import net.execheinz.upgrader.doublegame.DoubleGame;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ServerboundDoubleBetPacket(int colorOrdinal, long amount) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.colorOrdinal);
        buf.writeVarLong(this.amount);
    }

    public static ServerboundDoubleBetPacket decode(FriendlyByteBuf buf) {
        return new ServerboundDoubleBetPacket(buf.readVarInt(), buf.readVarLong());
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            DoubleGame game = DoubleGame.get();
            if (player == null || game == null) {
                return;
            }
            DoubleColor color = DoubleColor.byOrdinalSafe(this.colorOrdinal);
            game.placeBet(player, color, this.amount);
        });
        ctx.setPacketHandled(true);
    }
}
