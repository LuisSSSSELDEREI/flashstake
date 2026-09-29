package net.execheinz.upgrader.registry;

import net.execheinz.upgrader.menu.ArenaMenu;
import net.execheinz.upgrader.menu.CasesMenu;
import net.execheinz.upgrader.menu.DoubleMenu;
import net.execheinz.upgrader.menu.MarketMenu;
import net.execheinz.upgrader.menu.UpgraderMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "flashstake");
    public static final RegistryObject<MenuType<UpgraderMenu>> UPGRADER = MENUS.register("main", () -> IForgeMenuType.create((windowId, inv, data) -> new UpgraderMenu(windowId, inv)));
    public static final RegistryObject<MenuType<MarketMenu>> MARKET = MENUS.register("market", () -> IForgeMenuType.create((windowId, inv, data) -> new MarketMenu(windowId, inv)));
    public static final RegistryObject<MenuType<CasesMenu>> CASES = MENUS.register("cases", () -> IForgeMenuType.create((windowId, inv, data) -> new CasesMenu(windowId, inv)));
    public static final RegistryObject<MenuType<DoubleMenu>> DOUBLE = MENUS.register("double", () -> IForgeMenuType.create((windowId, inv, data) -> new DoubleMenu(windowId, inv)));
    public static final RegistryObject<MenuType<ArenaMenu>> ARENA = MENUS.register("arena", () -> IForgeMenuType.create((windowId, inv, data) -> new ArenaMenu(windowId, inv)));

    private ModMenus() {
    }
}
