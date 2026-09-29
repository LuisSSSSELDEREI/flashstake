package net.execheinz.upgrader.network;

import net.execheinz.upgrader.client.ClientPacketHandler;
import net.execheinz.upgrader.duel.DuelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

public record ClientboundDuelStatePacket(
    boolean active,
    int phaseOrdinal,
    int ticksLeft,
    long stake,
    long payout,
    String hostName,
    String guestName,
    int winner,
    String hostUuid
) {
    public static ClientboundDuelStatePacket idle() {
        return new ClientboundDuelStatePacket(false, 0, 0, 0L, 0L, "", "", 0, "");
    }

    public static ClientboundDuelStatePacket from(DuelManager.Session s) {
        return new ClientboundDuelStatePacket(
            true,
            s.phase.ordinal(),
            s.ticksLeft,
            s.stake,
            s.payout,
            s.hostName,
            s.guestName == null ? "" : s.guestName,
            s.winner,
            s.hostId.toString()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.active);
        buf.writeVarInt(this.phaseOrdinal);
        buf.writeVarInt(this.ticksLeft);
        buf.writeVarLong(this.stake);
        buf.writeVarLong(this.payout);
        buf.writeUtf(this.hostName == null ? "" : this.hostName, 32);
        buf.writeUtf(this.guestName == null ? "" : this.guestName, 32);
        buf.writeVarInt(this.winner);
        buf.writeUtf(this.hostUuid == null ? "" : this.hostUuid, 64);
    }

    public static ClientboundDuelStatePacket decode(FriendlyByteBuf buf) {
        return new ClientboundDuelStatePacket(
            buf.readBoolean(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarLong(),
            buf.readVarLong(),
            buf.readUtf(32),
            buf.readUtf(32),
            buf.readVarInt(),
            buf.readUtf(64)
        );
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleDuelState(this)));
        ctx.setPacketHandled(true);
    }
}
