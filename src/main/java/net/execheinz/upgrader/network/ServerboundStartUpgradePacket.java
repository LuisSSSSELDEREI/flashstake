package net.execheinz.upgrader.network;

import net.execheinz.upgrader.menu.UpgraderMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ServerboundStartUpgradePacket(boolean fast) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.fast);
    }

    public static ServerboundStartUpgradePacket decode(FriendlyByteBuf buf) {
        return new ServerboundStartUpgradePacket(buf.readBoolean());
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            AbstractContainerMenu open = player.containerMenu;
            if (open instanceof UpgraderMenu menu) {
                menu.startUpgrade(player, this.fast);
            }
        });
        ctx.setPacketHandled(true);
    }
}
