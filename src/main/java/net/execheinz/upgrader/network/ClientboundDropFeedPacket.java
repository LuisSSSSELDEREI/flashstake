package net.execheinz.upgrader.network;

import java.util.ArrayList;
import java.util.List;
import net.execheinz.upgrader.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ClientboundDropFeedPacket(List<Entry> entries) {
    public record Entry(String playerName, String caseId, ItemStack stack, long value, int tierOrdinal) {
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entries.size());
        for (Entry e : this.entries) {
            buf.writeUtf(e.playerName(), 32);
            buf.writeUtf(e.caseId(), 64);
            buf.writeJsonWithCodec(ItemStack.OPTIONAL_CODEC, e.stack());
            buf.writeVarLong(e.value());
            buf.writeVarInt(e.tierOrdinal());
        }
    }

    public static ClientboundDropFeedPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        ArrayList<Entry> entries = new ArrayList<>(n);
        for (int i = 0; i < n; ++i) {
            entries.add(new Entry(buf.readUtf(32), buf.readUtf(64), buf.readJsonWithCodec(ItemStack.OPTIONAL_CODEC), buf.readVarLong(), buf.readVarInt()));
        }
        return new ClientboundDropFeedPacket(entries);
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleDropFeed(this)));
        ctx.setPacketHandled(true);
    }
}
