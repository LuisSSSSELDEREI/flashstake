package net.execheinz.upgrader.network;

import net.execheinz.upgrader.Config;
import net.execheinz.upgrader.economy.MarketSellQuota;
import net.execheinz.upgrader.economy.MarketStock;
import net.execheinz.upgrader.economy.PlayerBalance;
import net.execheinz.upgrader.value.ItemValues;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.registries.ForgeRegistries;

public record ServerboundMarketBuyPacket(String itemId, int count) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.itemId);
        buf.writeVarInt(this.count);
    }

    public static ServerboundMarketBuyPacket decode(FriendlyByteBuf buf) {
        return new ServerboundMarketBuyPacket(buf.readUtf(), buf.readVarInt());
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || this.itemId.isEmpty() || this.count <= 0) {
                return;
            }
            ResourceLocation key = ResourceLocation.tryParse(this.itemId);
            if (key == null) {
                return;
            }
            Item item = ForgeRegistries.ITEMS.getValue(key);
            if (item == null || ItemValues.isBlacklisted(item)) {
                return;
            }
            int want = Math.max(1, this.count);
            int hardCap = Math.max(item.getDefaultMaxStackSize(), item.getDefaultMaxStackSize() * 27);

            MarketStock market = MarketStock.get(player.server);
            market.ensureReady(player.server);
            int available = market.available(item);
            if (available <= 0) {
                player.sendSystemMessage(Component.translatable("gui.flashstake.market.out_of_stock"));
                market.syncTo(player);
                return;
            }
            int amount = Math.min(want, Math.min(available, hardCap));

            long unit = Math.round(ItemValues.unitValue(player.level(), item) * Config.marketBuyRate);
            long cost = unit * (long) amount;
            if (cost <= 0L || !PlayerBalance.trySpend(player, cost)) {
                player.sendSystemMessage(Component.translatable("gui.flashstake.market.cannot_afford"));
                return;
            }
            if (!market.tryTake(item, amount)) {
                PlayerBalance.add(player, cost);
                player.sendSystemMessage(Component.translatable("gui.flashstake.market.out_of_stock"));
                market.syncTo(player);
                return;
            }

            int left = amount;
            while (left > 0) {
                int slice = Math.min(left, item.getDefaultMaxStackSize());
                ItemStack stack = new ItemStack(item, slice);
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
                left -= slice;
            }
            market.syncAll(player.server);
        });
        ctx.setPacketHandled(true);
    }
}
