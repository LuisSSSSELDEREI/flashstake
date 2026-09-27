package net.execheinz.upgrader.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.execheinz.upgrader.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundDoubleStatePacket(
    long roundId,
    int phaseOrdinal,
    int ticksLeft,
    int resultOrdinal,
    List<BetView> bets,
    List<Integer> history
) {
    public record BetView(String playerName, int colorOrdinal, long amount) {
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(this.roundId);
        buf.writeVarInt(this.phaseOrdinal);
        buf.writeVarInt(this.ticksLeft);
        buf.writeVarInt(this.resultOrdinal);
        buf.writeVarInt(this.bets.size());
        for (BetView bet : this.bets) {
            buf.writeUtf(bet.playerName(), 32);
            buf.writeVarInt(bet.colorOrdinal());
            buf.writeVarLong(bet.amount());
        }
        buf.writeVarInt(this.history.size());
        for (int color : this.history) {
            buf.writeVarInt(color);
        }
    }

    public static ClientboundDoubleStatePacket decode(FriendlyByteBuf buf) {
        long roundId = buf.readLong();
        int phase = buf.readVarInt();
        int ticks = buf.readVarInt();
        int result = buf.readVarInt();
        int n = buf.readVarInt();
        ArrayList<BetView> bets = new ArrayList<>(n);
        for (int i = 0; i < n; ++i) {
            bets.add(new BetView(buf.readUtf(32), buf.readVarInt(), buf.readVarLong()));
        }
        int h = buf.readVarInt();
        ArrayList<Integer> history = new ArrayList<>(h);
        for (int i = 0; i < h; ++i) {
            history.add(buf.readVarInt());
        }
        return new ClientboundDoubleStatePacket(roundId, phase, ticks, result, bets, history);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleDoubleState(this)));
        ctx.setPacketHandled(true);
    }
}
