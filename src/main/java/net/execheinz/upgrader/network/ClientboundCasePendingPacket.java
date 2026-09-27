package net.execheinz.upgrader.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.execheinz.upgrader.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundCasePendingPacket(List<ItemStack> stacks) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.stacks.size());
        for (ItemStack stack : this.stacks) {
            buf.writeItem(stack);
        }
    }

    public static ClientboundCasePendingPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        ArrayList<ItemStack> stacks = new ArrayList<>(n);
        for (int i = 0; i < n; ++i) {
            stacks.add(buf.readItem());
        }
        return new ClientboundCasePendingPacket(stacks);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleCasePending(this)));
        ctx.setPacketHandled(true);
    }
}
