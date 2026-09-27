package net.execheinz.upgrader.network;

import java.util.HashMap;
import java.util.Map;
import net.execheinz.upgrader.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ClientboundMarketStatePacket(
    Map<String, Integer> stock,
    long refreshInMs,
    int sellRemaining,
    long sellWindowMsLeft
) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.stock.size());
        for (Map.Entry<String, Integer> e : this.stock.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeVarInt(e.getValue());
        }
        buf.writeVarLong(this.refreshInMs);
        buf.writeVarInt(this.sellRemaining);
        buf.writeVarLong(this.sellWindowMsLeft);
    }

    public static ClientboundMarketStatePacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        HashMap<String, Integer> stock = new HashMap<>(Math.max(16, n * 2));
        for (int i = 0; i < n; ++i) {
            stock.put(buf.readUtf(), buf.readVarInt());
        }
        return new ClientboundMarketStatePacket(stock, buf.readVarLong(), buf.readVarInt(), buf.readVarLong());
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleMarketState(this)));
        ctx.setPacketHandled(true);
    }
}
