package net.execheinz.upgrader;

import net.execheinz.upgrader.Config;
import net.execheinz.upgrader.network.ModNetwork;
import net.execheinz.upgrader.registry.ModMenus;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(value = "flashstake")
public class Upgrader {
    public static final String MODID = "flashstake";

    public Upgrader() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModMenus.MENUS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, (IConfigSpec) Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
