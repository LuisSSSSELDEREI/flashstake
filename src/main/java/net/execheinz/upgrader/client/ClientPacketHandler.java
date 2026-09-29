package net.execheinz.upgrader.client;

import net.execheinz.upgrader.menu.UpgraderMenu;
import net.execheinz.upgrader.network.ClientboundArenaLobbyPacket;
import net.execheinz.upgrader.network.ClientboundBalancePacket;
import net.execheinz.upgrader.network.ClientboundBattleStatePacket;
import net.execheinz.upgrader.network.ClientboundCasePendingPacket;
import net.execheinz.upgrader.network.ClientboundCaseResultPacket;
import net.execheinz.upgrader.network.ClientboundCaseStashPacket;
import net.execheinz.upgrader.network.ClientboundContractResultPacket;
import net.execheinz.upgrader.network.ClientboundDoubleStatePacket;
import net.execheinz.upgrader.network.ClientboundDropFeedPacket;
import net.execheinz.upgrader.network.ClientboundDuelStatePacket;
import net.execheinz.upgrader.network.ClientboundMarketStatePacket;
import net.execheinz.upgrader.network.ClientboundUpgradeResultPacket;
import net.execheinz.upgrader.network.ClientboundUpgraderSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

public final class ClientPacketHandler {
    public static void handleSync(ClientboundUpgraderSyncPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            AbstractContainerMenu open = minecraft.player.containerMenu;
            if (open instanceof UpgraderMenu menu) {
                menu.setTargetClient(resolve(packet.targetId()), packet.targetCount());
            }
        }
        Screen screen = minecraft.screen;
        if (screen instanceof UpgraderScreen upgraderScreen) {
            upgraderScreen.onSync(packet);
        }
    }

    public static void handleResult(ClientboundUpgradeResultPacket packet) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof UpgraderScreen upgraderScreen) {
            upgraderScreen.onResult(packet);
        }
    }

    public static void handleBalance(ClientboundBalancePacket packet) {
        MarketScreen.setClientBalance(packet.balance());
        UiMotion.onBalance(packet.balance());
    }

    public static void handlePriceTable(net.execheinz.upgrader.network.ClientboundPriceTablePacket packet) {
        net.execheinz.upgrader.value.ItemValues.applySynced(packet.values());
        MarketScreen.clearSharedCatalog();
    }

    public static void handleMarketState(ClientboundMarketStatePacket packet) {
        // Refresh craft prices once the integrated server is up (1.21.4 client has no recipe list).
        net.execheinz.upgrader.value.ItemValues.invalidate();
        MarketScreen.clearSharedCatalog();
        MarketScreen.applyMarketState(packet);
    }

    public static void handleCaseResult(ClientboundCaseResultPacket packet) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof CasesScreen casesScreen) {
            casesScreen.onCaseResult(packet);
        }
    }

    public static void handleCaseStash(ClientboundCaseStashPacket packet) {
        CasesScreen.setClientStash(packet.slots());
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof CasesScreen casesScreen) {
            casesScreen.onStashSync(packet);
        }
    }

    public static void handleCasePending(ClientboundCasePendingPacket packet) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof CasesScreen casesScreen) {
            casesScreen.onPendingSync(packet.stacks());
        } else {
            CasesScreen.setClientPending(packet.stacks());
        }
    }

    public static void handleDoubleState(ClientboundDoubleStatePacket packet) {
        DoubleScreen.applyState(packet);
    }

    public static void handleDropFeed(ClientboundDropFeedPacket packet) {
        DropFeedClient.push(packet);
    }

    public static void handleArenaLobby(ClientboundArenaLobbyPacket packet) {
        ArenaScreen.applyLobby(packet);
    }

    public static void handleBattleState(ClientboundBattleStatePacket packet) {
        ArenaScreen.applyBattle(packet);
    }

    public static void handleDuelState(ClientboundDuelStatePacket packet) {
        ArenaScreen.applyDuel(packet);
    }

    public static void handleContractResult(ClientboundContractResultPacket packet) {
        ArenaScreen.applyContract(packet);
        if (packet.ok()) {
            if (packet.rewardValue() >= packet.inputValue()) {
                UiMotion.celebrateWin(packet.rewardValue() > packet.inputValue() * 1.2);
            } else {
                UiMotion.celebrateLose();
            }
        }
    }

    private static Item resolve(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? null : ForgeRegistries.ITEMS.getValue(key);
    }

    private ClientPacketHandler() {
    }
}
