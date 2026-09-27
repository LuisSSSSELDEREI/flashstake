package net.execheinz.upgrader.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.execheinz.upgrader.network.ModNetwork;
import net.execheinz.upgrader.network.ServerboundOpenUpgraderPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class ModKeybinds {
    public static final String CATEGORY = "key.categories.flashstake";

    public static final KeyMapping OPEN_UPGRADER = new KeyMapping(
        "key.flashstake.open",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_U,
        CATEGORY
    );

    private ModKeybinds() {
    }

    @Mod.EventBusSubscriber(modid = "flashstake", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_UPGRADER);
        }
    }

    @Mod.EventBusSubscriber(modid = "flashstake", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBus {
        @SubscribeEvent
        static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null || mc.getConnection() == null) {
                return;
            }
            while (OPEN_UPGRADER.consumeClick()) {
                ModNetwork.sendToServer(new ServerboundOpenUpgraderPacket());
            }
        }
    }
}
