package net.execheinz.upgrader.network;

import java.util.ArrayList;
import java.util.List;
import net.execheinz.upgrader.cases.CaseDefinition;
import net.execheinz.upgrader.cases.CaseLoot;
import net.execheinz.upgrader.economy.PlayerBalance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ServerboundOpenCasePacket(String caseId, int count, boolean fast) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.caseId, 64);
        buf.writeVarInt(this.count);
        buf.writeBoolean(this.fast);
    }

    public static ServerboundOpenCasePacket decode(FriendlyByteBuf buf) {
        return new ServerboundOpenCasePacket(buf.readUtf(64), buf.readVarInt(), buf.readBoolean());
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            CaseDefinition def = CaseDefinition.byId(this.caseId);
            if (def == null) {
                return;
            }
            int n = Math.max(1, Math.min(5, this.count));
            long cost = def.price() * (long) n;
            if (!PlayerBalance.trySpend(player, cost)) {
                return;
            }
            List<ItemStack> rewards = CaseLoot.rollMany(player.level(), def, player.getRandom(), n);
            // Auto-keep previous undecided loot, then hold the new drop for Keep/Sell
            net.execheinz.upgrader.economy.CasePending.autoKeepIfAny(player);
            net.execheinz.upgrader.economy.CasePending.set(player, rewards);
            ModNetwork.sendTo(player, new ClientboundCaseResultPacket(this.caseId, this.fast, new ArrayList<>(rewards)));
            net.execheinz.upgrader.economy.DropFeed.maybeBroadcast(player, this.caseId, rewards);
        });
        ctx.setPacketHandled(true);
    }
}
