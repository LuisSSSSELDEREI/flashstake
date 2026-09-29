package net.execheinz.upgrader.client;

import java.util.HashMap;
import java.util.Map;
import net.execheinz.upgrader.cases.CaseDefinition;
import net.minecraft.client.Minecraft;
import net.execheinz.upgrader.client.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Shared cover-crop blit for case art (Cases + Arena battle picker). */
public final class CaseArt {
    private static final Map<ResourceLocation, Boolean> CACHE = new HashMap<>();

    private CaseArt() {
    }

    public static boolean blitCover(GuiGraphics graphics, CaseDefinition def, int x, int y, int w, int h) {
        return blitCover(graphics, def, x, y, w, h, 0.0);
    }

    /**
     * Cover-crop case art into {@code w×h}. {@code biasX} in [-1,1] shifts the crop.
     */
    public static boolean blitCover(GuiGraphics graphics, CaseDefinition def, int x, int y, int w, int h, double biasX) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || def == null || w <= 0 || h <= 0) {
            return false;
        }
        ResourceLocation icon = def.icon();
        boolean present = CACHE.computeIfAbsent(icon, key -> mc.getResourceManager().getResource(key).isPresent());
        if (!present) {
            return false;
        }
        int texW = CaseDefinition.ICON_WIDTH;
        int texH = CaseDefinition.ICON_HEIGHT;
        double boxAspect = (double) w / (double) h;
        double texAspect = (double) texW / (double) texH;
        int srcW;
        int srcH;
        if (texAspect > boxAspect) {
            srcH = texH;
            srcW = (int) Math.round(texH * boxAspect);
        } else {
            srcW = texW;
            srcH = (int) Math.round(texW / boxAspect);
        }
        int maxU = Math.max(0, texW - srcW);
        int maxV = Math.max(0, texH - srcH);
        int u = (int) Math.round(maxU * (0.5 - Math.max(-1.0, Math.min(1.0, biasX)) * 0.5));
        u = Math.max(0, Math.min(maxU, u));
        int v = maxV / 2;
        graphics.blit(icon, x, y, w, h, u, v, srcW, srcH, texW, texH);
        return true;
    }
}
