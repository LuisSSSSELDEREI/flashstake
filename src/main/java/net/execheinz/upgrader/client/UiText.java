package net.execheinz.upgrader.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Crisp scaled UI text — vanilla font, slight scale + shadow for clarity. */
public final class UiText {
    private UiText() {
    }

    public static void draw(GuiGraphics g, Font font, Component text, int x, int y, int color, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    public static void drawShadow(GuiGraphics g, Font font, Component text, int x, int y, int color, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.drawString(font, text, 1, 1, 0x88000000, false);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    public static void drawCentered(GuiGraphics g, Font font, Component text, int cx, int y, int color, float scale) {
        int w = Math.round(font.width(text) * scale);
        drawShadow(g, font, text, cx - w / 2, y, color, scale);
    }

    public static void drawCenteredRaw(GuiGraphics g, Font font, String text, int cx, int y, int color, float scale) {
        drawCentered(g, font, Component.literal(text), cx, y, color, scale);
    }

    public static String ellipsize(Font font, String text, int maxPx) {
        if (font.width(text) <= maxPx) {
            return text;
        }
        String dots = "...";
        int budget = Math.max(0, maxPx - font.width(dots));
        if (budget <= 0) {
            return dots;
        }
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end)) > budget) {
            end--;
        }
        return text.substring(0, Mth.clamp(end, 0, text.length())) + dots;
    }
}
