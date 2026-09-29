package net.execheinz.upgrader.network;

import java.util.ArrayList;
import java.util.List;
import net.execheinz.upgrader.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;

public record ClientboundArenaLobbyPacket(
    List<PlayerView> players,
    List<BattleInvite> battles,
    List<DuelInvite> duels
) {
    public record PlayerView(String uuid, String name) {
    }

    public record BattleInvite(String hostUuid, String hostName, String caseId, long price) {
    }

    public record DuelInvite(String hostUuid, String hostName, long stake) {
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.players.size());
        for (PlayerView p : this.players) {
            buf.writeUtf(p.uuid(), 64);
            buf.writeUtf(p.name(), 32);
        }
        buf.writeVarInt(this.battles.size());
        for (BattleInvite b : this.battles) {
            buf.writeUtf(b.hostUuid(), 64);
            buf.writeUtf(b.hostName(), 32);
            buf.writeUtf(b.caseId(), 64);
            buf.writeVarLong(b.price());
        }
        buf.writeVarInt(this.duels.size());
        for (DuelInvite d : this.duels) {
            buf.writeUtf(d.hostUuid(), 64);
            buf.writeUtf(d.hostName(), 32);
            buf.writeVarLong(d.stake());
        }
    }

    public static ClientboundArenaLobbyPacket decode(FriendlyByteBuf buf) {
        int pn = buf.readVarInt();
        ArrayList<PlayerView> players = new ArrayList<>(pn);
        for (int i = 0; i < pn; ++i) {
            players.add(new PlayerView(buf.readUtf(64), buf.readUtf(32)));
        }
        int bn = buf.readVarInt();
        ArrayList<BattleInvite> battles = new ArrayList<>(bn);
        for (int i = 0; i < bn; ++i) {
            battles.add(new BattleInvite(buf.readUtf(64), buf.readUtf(32), buf.readUtf(64), buf.readVarLong()));
        }
        int dn = buf.readVarInt();
        ArrayList<DuelInvite> duels = new ArrayList<>(dn);
        for (int i = 0; i < dn; ++i) {
            duels.add(new DuelInvite(buf.readUtf(64), buf.readUtf(32), buf.readVarLong()));
        }
        return new ClientboundArenaLobbyPacket(players, battles, duels);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleArenaLobby(this)));
        ctx.setPacketHandled(true);
    }
}
