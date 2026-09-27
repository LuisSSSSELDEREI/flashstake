package net.execheinz.upgrader.network;

import java.util.function.Supplier;
import net.execheinz.upgrader.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundBalancePacket(long balance) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarLong(this.balance);
    }

    public static ClientboundBalancePacket decode(FriendlyByteBuf buf) {
        return new ClientboundBalancePacket(buf.readVarLong());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleBalance(this)));
        ctx.setPacketHandled(true);
    }
}
