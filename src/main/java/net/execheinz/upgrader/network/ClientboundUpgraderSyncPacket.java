package net.execheinz.upgrader.network;

import net.execheinz.upgrader.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;

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

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSync(this)));
        ctx.setPacketHandled(true);
    }
}
