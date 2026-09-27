package net.execheinz.upgrader.network;

import java.util.function.Supplier;
import net.execheinz.upgrader.menu.UpgraderMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

public record ServerboundSetTargetPacket(String itemId, int count) {
    public static ServerboundSetTargetPacket of(Item item, int count) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return new ServerboundSetTargetPacket(key == null ? "" : key.toString(), UpgraderMenu.clampCount(item, count));
    }

    public static ServerboundSetTargetPacket clear() {
        return new ServerboundSetTargetPacket("", 1);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.itemId, 256);
        buf.writeVarInt(this.count);
    }

    public static ServerboundSetTargetPacket decode(FriendlyByteBuf buf) {
        return new ServerboundSetTargetPacket(buf.readUtf(256), buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            AbstractContainerMenu open = player.containerMenu;
            if (!(open instanceof UpgraderMenu menu)) {
                return;
            }
            Item item = null;
            if (!this.itemId.isEmpty()) {
                ResourceLocation key = ResourceLocation.tryParse(this.itemId);
                if (key == null) {
                    return;
                }
                item = ForgeRegistries.ITEMS.getValue(key);
                if (item == null) {
                    return;
                }
            }
            menu.setTarget(player, item, this.count);
        });
        ctx.setPacketHandled(true);
    }
}
