package net.execheinz.upgrader.network;

import java.util.ArrayList;
import java.util.List;
import net.execheinz.upgrader.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ClientboundCaseResultPacket(String caseId, boolean fast, List<ItemStack> rewards) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.caseId, 64);
        buf.writeBoolean(this.fast);
        buf.writeVarInt(this.rewards.size());
        for (ItemStack stack : this.rewards) {
            buf.writeItem(stack);
        }
    }

    public static ClientboundCaseResultPacket decode(FriendlyByteBuf buf) {
        String id = buf.readUtf(64);
        boolean fast = buf.readBoolean();
        int n = buf.readVarInt();
        ArrayList<ItemStack> rewards = new ArrayList<>(n);
        for (int i = 0; i < n; ++i) {
            rewards.add(buf.readItem());
        }
        return new ClientboundCaseResultPacket(id, fast, rewards);
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleCaseResult(this)));
        ctx.setPacketHandled(true);
    }
}
