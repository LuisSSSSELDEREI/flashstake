package net.execheinz.upgrader.client;

import net.execheinz.upgrader.client.MarketScreen;
import net.execheinz.upgrader.client.UpgraderScreen;
import net.execheinz.upgrader.registry.ModMenus;
import net.execheinz.upgrader.value.ItemValues;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientSetup {
    private ClientSetup() {
    }

    @Mod.EventBusSubscriber(modid = "flashstake", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBus {
        private static int warmupCooldown;

        @SubscribeEvent
        static void onRecipesUpdated(RecipesUpdatedEvent event) {
            ItemValues.invalidate();
            MarketScreen.clearSharedCatalog();
            warmupCooldown = 0;
        }

        @SubscribeEvent
        static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ItemValues.clearSynced();
            MarketScreen.clearSharedCatalog();
            UiMotion.resetBalance();
            warmupCooldown = 0;
        }

        @SubscribeEvent
        static void onClientTick(TickEvent.ClientTickEvent.Post event) {
            if (ItemValues.isWarm()) {
                return;
            }
            if (warmupCooldown > 0) {
                warmupCooldown--;
                return;
            }
            warmupCooldown = 20;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                return;
            }
            // Only warm when integrated-server recipes are available.
            if (ClientRecipes.tryGet() == null) {
                return;
            }
            ItemValues.warmup(mc.level);
        }
    }

    @Mod.EventBusSubscriber(modid = "flashstake", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MenuScreens.register(ModMenus.UPGRADER.get(), UpgraderScreen::new);
                MenuScreens.register(ModMenus.MARKET.get(), MarketScreen::new);
                MenuScreens.register(ModMenus.CASES.get(), CasesScreen::new);
                MenuScreens.register(ModMenus.DOUBLE.get(), DoubleScreen::new);
                MenuScreens.register(ModMenus.ARENA.get(), ArenaScreen::new);
            });
        }
    }
}
