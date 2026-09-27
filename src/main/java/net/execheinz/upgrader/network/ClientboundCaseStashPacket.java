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

public record ClientboundCaseStashPacket(List<ItemStack> slots) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.slots.size());
        for (ItemStack stack : this.slots) {
            buf.writeItem(stack);
        }
    }

    public static ClientboundCaseStashPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        ArrayList<ItemStack> slots = new ArrayList<>(n);
        for (int i = 0; i < n; ++i) {
            slots.add(buf.readItem());
        }
        return new ClientboundCaseStashPacket(slots);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleCaseStash(this)));
        ctx.setPacketHandled(true);
    }
}
