package net.execheinz.upgrader.network;

import net.execheinz.upgrader.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ClientboundBalancePacket(long balance) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarLong(this.balance);
    }

    public static ClientboundBalancePacket decode(FriendlyByteBuf buf) {
        return new ClientboundBalancePacket(buf.readVarLong());
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleBalance(this)));
        ctx.setPacketHandled(true);
    }
}
