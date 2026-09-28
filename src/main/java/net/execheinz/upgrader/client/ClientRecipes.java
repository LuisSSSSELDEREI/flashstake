package net.execheinz.upgrader.client;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 1.21.4 clients no longer receive the full RecipeManager — only the integrated
 * server still has getRecipes(). Market/UI pricing runs on the client thread, so
 * we pull recipes from the local integrated server when available.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientRecipes {
    private ClientRecipes() {
    }

    @Nullable
    public static RecipeManager tryGet() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return null;
        }
        IntegratedServer server = mc.getSingleplayerServer();
        return server == null ? null : server.getRecipeManager();
    }
}
