package net.execheinz.upgrader.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Shrinks FlashStake panels so they stay readable on high GUI Scale.
 * Fits the panel into the scaled screen AND caps physical size relative
 * to GUI Scale 3 (scale 4-5 no longer blows the window up).
 */
public final class UiFit {
    private static final int PAD = 8;
    /** Comfortable reference GUI scale - higher scales shrink the UI. */
    private static final double REF_GUI_SCALE = 3.0;

    private UiFit() {
    }

    public static float scale(int panelW, int panelH, int screenW, int screenH) {
        double guiScale = REF_GUI_SCALE;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getWindow() != null) {
                guiScale = mc.getWindow().getGuiScale();
            }
        } catch (Throwable ignored) {
        }
        float sx = (screenW - PAD * 2f) / Math.max(1, panelW);
        float sy = (screenH - PAD * 2f) / Math.max(1, panelH);
        float fit = Math.min(sx, sy);
        if (fit >= 0.999f) {
            fit = 1.0f;
        } else {
            fit = Math.max(0.35f, fit);
        }
        float comfort = (float) (REF_GUI_SCALE / Math.max(1.0, guiScale));
        if (comfort > 0.999f) {
            comfort = 1.0f;
        } else {
            comfort = Math.max(0.35f, comfort);
        }
        return Math.min(fit, comfort);
    }

    public static void push(GuiGraphics graphics, int left, int top, int panelW, int panelH, float scale) {
        if (scale >= 0.999f) {
            return;
        }
        float cx = left + panelW * 0.5f;
        float cy = top + panelH * 0.5f;
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0.0f);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.pose().translate(-cx, -cy, 0.0f);
    }

    public static void pop(GuiGraphics graphics, float scale) {
        if (scale >= 0.999f) {
            return;
        }
        graphics.pose().popPose();
    }

    public static double mouseX(double mouseX, int left, int panelW, float scale) {
        if (scale >= 0.999f) {
            return mouseX;
        }
        float cx = left + panelW * 0.5f;
        return (mouseX - cx) / scale + cx;
    }

    public static double mouseY(double mouseY, int top, int panelH, float scale) {
        if (scale >= 0.999f) {
            return mouseY;
        }
        float cy = top + panelH * 0.5f;
        return (mouseY - cy) / scale + cy;
    }

    public static int mouseXi(int mouseX, int left, int panelW, float scale) {
        return (int) Math.round(mouseX(mouseX, left, panelW, scale));
    }

    public static int mouseYi(int mouseY, int top, int panelH, float scale) {
        return (int) Math.round(mouseY(mouseY, top, panelH, scale));
    }

    public static double delta(double screenDelta, float scale) {
        return scale >= 0.999f ? screenDelta : screenDelta / scale;
    }

}