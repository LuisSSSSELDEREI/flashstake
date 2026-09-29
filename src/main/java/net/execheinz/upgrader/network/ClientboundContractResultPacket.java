package net.execheinz.upgrader.network;

import net.execheinz.upgrader.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

/**
 * Contract result + compact slot deltas (not a full inventory snapshot).
 * Full 36× ItemStack JSON payloads blow custom_payload limits and kick the player.
 */
public record ClientboundContractResultPacket(
    boolean ok,
    long inputValue,
    long rewardValue,
    ItemStack reward,
    String caseId,
    int[] changedSlots,
    ItemStack[] changedStacks
) {
    public static final int[] NO_SLOTS = new int[0];
    public static final ItemStack[] NO_STACKS = new ItemStack[0];

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.ok);
        buf.writeVarLong(this.inputValue);
        buf.writeVarLong(this.rewardValue);
        buf.writeJsonWithCodec(ItemStack.OPTIONAL_CODEC, this.reward == null ? ItemStack.EMPTY : this.reward);
        buf.writeUtf(this.caseId == null ? "" : this.caseId, 64);
        int n = this.changedSlots == null ? 0 : Math.min(this.changedSlots.length, 36);
        buf.writeVarInt(n);
        for (int i = 0; i < n; ++i) {
            buf.writeVarInt(this.changedSlots[i]);
            ItemStack s = this.changedStacks != null && i < this.changedStacks.length && this.changedStacks[i] != null
                ? this.changedStacks[i]
                : ItemStack.EMPTY;
            buf.writeJsonWithCodec(ItemStack.OPTIONAL_CODEC, s);
        }
    }

    public static ClientboundContractResultPacket decode(FriendlyByteBuf buf) {
        boolean ok = buf.readBoolean();
        long inputValue = buf.readVarLong();
        long rewardValue = buf.readVarLong();
        ItemStack reward = buf.readJsonWithCodec(ItemStack.OPTIONAL_CODEC);
        String caseId = buf.readUtf(64);
        int n = Math.min(36, Math.max(0, buf.readVarInt()));
        int[] slots = new int[n];
        ItemStack[] stacks = new ItemStack[n];
        for (int i = 0; i < n; ++i) {
            slots[i] = buf.readVarInt();
            stacks[i] = buf.readJsonWithCodec(ItemStack.OPTIONAL_CODEC);
        }
        return new ClientboundContractResultPacket(ok, inputValue, rewardValue, reward, caseId, slots, stacks);
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleContractResult(this)));
        ctx.setPacketHandled(true);
    }
}
