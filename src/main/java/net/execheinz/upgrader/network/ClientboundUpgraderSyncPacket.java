package net.execheinz.upgrader.network;

import java.util.function.Supplier;
import net.execheinz.upgrader.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundUpgraderSyncPacket(String targetId, long inputValue, long targetValue, float chance, int targetCount) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.targetId, 256);
        buf.writeVarLong(this.inputValue);
        buf.writeVarLong(this.targetValue);
        buf.writeFloat(this.chance);
        buf.writeVarInt(this.targetCount);
    }

    public static ClientboundUpgraderSyncPacket decode(FriendlyByteBuf buf) {
        return new ClientboundUpgraderSyncPacket(
            buf.readUtf(256),
            buf.readVarLong(),
            buf.readVarLong(),
            buf.readFloat(),
            buf.readVarInt()
        );
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSync(this)));
        ctx.setPacketHandled(true);
    }
}
