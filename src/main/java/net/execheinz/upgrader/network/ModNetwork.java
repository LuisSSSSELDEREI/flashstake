package net.execheinz.upgrader.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class ModNetwork {
    private static final int PROTOCOL = 15;
    public static final SimpleChannel CHANNEL = ChannelBuilder
        .named(new ResourceLocation("flashstake", "main"))
        .networkProtocolVersion(PROTOCOL)
        .acceptedVersions(Channel.VersionTest.exact(PROTOCOL))
        .simpleChannel();

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(ServerboundSetTargetPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundSetTargetPacket::encode).decoder(ServerboundSetTargetPacket::decode).consumerMainThread(ServerboundSetTargetPacket::handle).add();
        CHANNEL.messageBuilder(ServerboundStartUpgradePacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundStartUpgradePacket::encode).decoder(ServerboundStartUpgradePacket::decode).consumerMainThread(ServerboundStartUpgradePacket::handle).add();
        CHANNEL.messageBuilder(ClientboundUpgraderSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundUpgraderSyncPacket::encode).decoder(ClientboundUpgraderSyncPacket::decode).consumerMainThread(ClientboundUpgraderSyncPacket::handle).add();
        CHANNEL.messageBuilder(ClientboundUpgradeResultPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundUpgradeResultPacket::encode).decoder(ClientboundUpgradeResultPacket::decode).consumerMainThread(ClientboundUpgradeResultPacket::handle).add();
        CHANNEL.messageBuilder(ClientboundBalancePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundBalancePacket::encode).decoder(ClientboundBalancePacket::decode).consumerMainThread(ClientboundBalancePacket::handle).add();
        CHANNEL.messageBuilder(ServerboundOpenMarketPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundOpenMarketPacket::encode).decoder(ServerboundOpenMarketPacket::decode).consumerMainThread(ServerboundOpenMarketPacket::handle).add();
        CHANNEL.messageBuilder(ServerboundOpenUpgraderPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundOpenUpgraderPacket::encode).decoder(ServerboundOpenUpgraderPacket::decode).consumerMainThread(ServerboundOpenUpgraderPacket::handle).add();
        CHANNEL.messageBuilder(ServerboundMarketSellPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundMarketSellPacket::encode).decoder(ServerboundMarketSellPacket::decode).consumerMainThread(ServerboundMarketSellPacket::handle).add();
        CHANNEL.messageBuilder(ServerboundMarketBuyPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundMarketBuyPacket::encode).decoder(ServerboundMarketBuyPacket::decode).consumerMainThread(ServerboundMarketBuyPacket::handle).add();
        CHANNEL.messageBuilder(ServerboundOpenCasesPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundOpenCasesPacket::encode).decoder(ServerboundOpenCasesPacket::decode).consumerMainThread(ServerboundOpenCasesPacket::handle).add();
        CHANNEL.messageBuilder(ServerboundOpenCasePacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundOpenCasePacket::encode).decoder(ServerboundOpenCasePacket::decode).consumerMainThread(ServerboundOpenCasePacket::handle).add();
        CHANNEL.messageBuilder(ClientboundCaseResultPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundCaseResultPacket::encode).decoder(ClientboundCaseResultPacket::decode).consumerMainThread(ClientboundCaseResultPacket::handle).add();
        CHANNEL.messageBuilder(ClientboundCaseStashPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundCaseStashPacket::encode).decoder(ClientboundCaseStashPacket::decode).consumerMainThread(ClientboundCaseStashPacket::handle).add();
        CHANNEL.messageBuilder(ServerboundCaseStashActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundCaseStashActionPacket::encode).decoder(ServerboundCaseStashActionPacket::decode).consumerMainThread(ServerboundCaseStashActionPacket::handle).add();
        CHANNEL.messageBuilder(ClientboundCasePendingPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundCasePendingPacket::encode).decoder(ClientboundCasePendingPacket::decode).consumerMainThread(ClientboundCasePendingPacket::handle).add();
        CHANNEL.messageBuilder(ServerboundCasePendingActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundCasePendingActionPacket::encode).decoder(ServerboundCasePendingActionPacket::decode).consumerMainThread(ServerboundCasePendingActionPacket::handle).add();
        CHANNEL.messageBuilder(ServerboundOpenDoublePacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundOpenDoublePacket::encode).decoder(ServerboundOpenDoublePacket::decode).consumerMainThread(ServerboundOpenDoublePacket::handle).add();
        CHANNEL.messageBuilder(ServerboundDoubleBetPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundDoubleBetPacket::encode).decoder(ServerboundDoubleBetPacket::decode).consumerMainThread(ServerboundDoubleBetPacket::handle).add();
        CHANNEL.messageBuilder(ClientboundDoubleStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundDoubleStatePacket::encode).decoder(ClientboundDoubleStatePacket::decode).consumerMainThread(ClientboundDoubleStatePacket::handle).add();
        CHANNEL.messageBuilder(ClientboundDropFeedPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundDropFeedPacket::encode).decoder(ClientboundDropFeedPacket::decode).consumerMainThread(ClientboundDropFeedPacket::handle).add();
        CHANNEL.messageBuilder(ClientboundMarketStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundMarketStatePacket::encode).decoder(ClientboundMarketStatePacket::decode).consumerMainThread(ClientboundMarketStatePacket::handle).add();
        CHANNEL.messageBuilder(ServerboundOpenArenaPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundOpenArenaPacket::encode).decoder(ServerboundOpenArenaPacket::decode).consumerMainThread(ServerboundOpenArenaPacket::handle).add();
        CHANNEL.messageBuilder(ServerboundArenaActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundArenaActionPacket::encode).decoder(ServerboundArenaActionPacket::decode).consumerMainThread(ServerboundArenaActionPacket::handle).add();
        CHANNEL.messageBuilder(ClientboundArenaLobbyPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundArenaLobbyPacket::encode).decoder(ClientboundArenaLobbyPacket::decode).consumerMainThread(ClientboundArenaLobbyPacket::handle).add();
        CHANNEL.messageBuilder(ClientboundBattleStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundBattleStatePacket::encode).decoder(ClientboundBattleStatePacket::decode).consumerMainThread(ClientboundBattleStatePacket::handle).add();
        CHANNEL.messageBuilder(ClientboundDuelStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundDuelStatePacket::encode).decoder(ClientboundDuelStatePacket::decode).consumerMainThread(ClientboundDuelStatePacket::handle).add();
        CHANNEL.messageBuilder(ClientboundContractResultPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundContractResultPacket::encode).decoder(ClientboundContractResultPacket::decode).consumerMainThread(ClientboundContractResultPacket::handle).add();
        CHANNEL.messageBuilder(ClientboundPriceTablePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundPriceTablePacket::encode).decoder(ClientboundPriceTablePacket::decode).consumerMainThread(ClientboundPriceTablePacket::handle).add();
    }

    public static void sendTo(ServerPlayer player, Object message) {
        CHANNEL.send(message, PacketDistributor.PLAYER.with(player));
    }

    public static void sendToAll(Object message) {
        CHANNEL.send(message, PacketDistributor.ALL.noArg());
    }

    public static void sendToServer(Object message) {
        CHANNEL.send(message, PacketDistributor.SERVER.noArg());
    }

    private ModNetwork() {
    }
}
