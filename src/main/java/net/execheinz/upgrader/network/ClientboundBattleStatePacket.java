package net.execheinz.upgrader.network;

import java.util.UUID;
import net.execheinz.upgrader.battle.BattleManager;
import net.execheinz.upgrader.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

public record ClientboundBattleStatePacket(
    boolean active,
    int phaseOrdinal,
    int ticksLeft,
    String caseId,
    long casePrice,
    String hostName,
    String guestName,
    ItemStack hostLoot,
    ItemStack guestLoot,
    long hostValue,
    long guestValue,
    int winner,
    String hostUuid,
    String selfUuidHint
) {
    public static ClientboundBattleStatePacket idle() {
        return new ClientboundBattleStatePacket(
            false, 0, 0, "", 0L, "", "", ItemStack.EMPTY, ItemStack.EMPTY, 0L, 0L, 2, "", ""
        );
    }

    public static ClientboundBattleStatePacket from(BattleManager.Session s) {
        return new ClientboundBattleStatePacket(
            true,
            s.phase.ordinal(),
            s.ticksLeft,
            s.caseId,
            s.casePrice,
            s.hostName,
            s.guestName == null ? "" : s.guestName,
            s.hostLoot == null ? ItemStack.EMPTY : s.hostLoot.copy(),
            s.guestLoot == null ? ItemStack.EMPTY : s.guestLoot.copy(),
            s.hostValue,
            s.guestValue,
            s.winner,
            s.hostId.toString(),
            ""
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.active);
        buf.writeVarInt(this.phaseOrdinal);
        buf.writeVarInt(this.ticksLeft);
        buf.writeUtf(this.caseId == null ? "" : this.caseId, 64);
        buf.writeVarLong(this.casePrice);
        buf.writeUtf(this.hostName == null ? "" : this.hostName, 32);
        buf.writeUtf(this.guestName == null ? "" : this.guestName, 32);
        buf.writeJsonWithCodec(ItemStack.OPTIONAL_CODEC, this.hostLoot == null ? ItemStack.EMPTY : this.hostLoot);
        buf.writeJsonWithCodec(ItemStack.OPTIONAL_CODEC, this.guestLoot == null ? ItemStack.EMPTY : this.guestLoot);
        buf.writeVarLong(this.hostValue);
        buf.writeVarLong(this.guestValue);
        buf.writeVarInt(this.winner);
        buf.writeUtf(this.hostUuid == null ? "" : this.hostUuid, 64);
        buf.writeUtf(this.selfUuidHint == null ? "" : this.selfUuidHint, 64);
    }

    public static ClientboundBattleStatePacket decode(FriendlyByteBuf buf) {
        return new ClientboundBattleStatePacket(
            buf.readBoolean(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readUtf(64),
            buf.readVarLong(),
            buf.readUtf(32),
            buf.readUtf(32),
            buf.readJsonWithCodec(ItemStack.OPTIONAL_CODEC),
            buf.readJsonWithCodec(ItemStack.OPTIONAL_CODEC),
            buf.readVarLong(),
            buf.readVarLong(),
            buf.readVarInt(),
            buf.readUtf(64),
            buf.readUtf(64)
        );
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleBattleState(this)));
        ctx.setPacketHandled(true);
    }
}
