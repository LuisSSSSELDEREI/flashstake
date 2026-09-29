package net.execheinz.upgrader.network;

import java.util.HashMap;
import java.util.Map;
import net.execheinz.upgrader.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Server-computed unit prices. Remote 1.21.4 clients get no recipe list, so they can't
 * derive craft prices themselves and would fall back to 1 for most items.
 */
public record ClientboundPriceTablePacket(Map<Item, Double> values) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.values.size());
        for (Map.Entry<Item, Double> e : this.values.entrySet()) {
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(e.getKey());
            buf.writeResourceLocation(key == null ? ResourceLocation.withDefaultNamespace("air") : key);
            buf.writeDouble(e.getValue());
        }
    }

    public static ClientboundPriceTablePacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        HashMap<Item, Double> map = new HashMap<>(Math.max(16, n * 2));
        for (int i = 0; i < n; ++i) {
            ResourceLocation key = buf.readResourceLocation();
            double value = buf.readDouble();
            Item item = ForgeRegistries.ITEMS.getValue(key);
            if (item != null) {
                map.put(item, value);
            }
        }
        return new ClientboundPriceTablePacket(map);
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handlePriceTable(this)));
        ctx.setPacketHandled(true);
    }
}
