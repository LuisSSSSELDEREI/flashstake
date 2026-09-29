package net.execheinz.upgrader.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/**
 * Fast GUI primitives — avoids per-scanline fills and dense decorative meshes.
 * Visual style kept; cost cut so menus stay smooth.
 */
public final class WheelRenderer {
    private static final float ARC_DEG_PER_SEG = 6.0f;
    private static final int ROUND_BANDS = 12;

    public static void arc(GuiGraphics graphics, float cx, float cy, float innerRadius, float outerRadius, float startDeg, float sweepDeg, int argb) {
        WheelRenderer.arc(graphics, cx, cy, innerRadius, outerRadius, startDeg, sweepDeg, argb, argb);
    }

    public static void arc(GuiGraphics graphics, float cx, float cy, float innerRadius, float outerRadius, float startDeg, float sweepDeg, int fromArgb, int toArgb) {
        if (sweepDeg <= 0.0f) {
            return;
        }
        sweepDeg = Math.min(sweepDeg, 360.0f);
        int segments = Math.max(2, Math.round(sweepDeg / ARC_DEG_PER_SEG));
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = WheelRenderer.begin(graphics, VertexFormat.Mode.TRIANGLE_STRIP);
        boolean solid = fromArgb == toArgb;
        for (int i = 0; i <= segments; ++i) {
            float t = (float) i / (float) segments;
            int color = solid ? fromArgb : WheelRenderer.lerpColor(fromArgb, toArgb, t);
            double angle = Math.toRadians(startDeg + sweepDeg * t);
            float sin = (float) Math.sin(angle);
            float cos = (float) Math.cos(angle);
            WheelRenderer.vertex(buffer, matrix, cx + sin * outerRadius, cy - cos * outerRadius, color);
            WheelRenderer.vertex(buffer, matrix, cx + sin * innerRadius, cy - cos * innerRadius, color);
        }
        WheelRenderer.end(buffer);
    }

    public static void disc(GuiGraphics graphics, float cx, float cy, float radius, int argb) {
        WheelRenderer.arc(graphics, cx, cy, 0.0f, radius, 0.0f, 360.0f, argb);
    }

    public static void tickRing(GuiGraphics graphics, float cx, float cy, float innerRadius, float outerRadius, float rotationDeg, int count, int majorEvery, int minorArgb, int majorArgb) {
        int ticks = Math.min(count, 36);
        int major = Math.max(1, majorEvery * count / Math.max(1, ticks));
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = WheelRenderer.begin(graphics, VertexFormat.Mode.QUADS);
        for (int i = 0; i < ticks; ++i) {
            boolean isMajor = i % major == 0;
            int color = isMajor ? majorArgb : minorArgb;
            float inner = isMajor ? innerRadius - 1.5f : innerRadius;
            float outer = isMajor ? outerRadius + 1.0f : outerRadius;
            float half = isMajor ? 0.9f : 0.5f;
            double angle = Math.toRadians(rotationDeg + 360.0f * (float) i / (float) ticks);
            float sin = (float) Math.sin(angle);
            float cos = (float) Math.cos(angle);
            float px = cos * half;
            float py = sin * half;
            WheelRenderer.vertex(buffer, matrix, cx + sin * outer + px, cy - cos * outer + py, color);
            WheelRenderer.vertex(buffer, matrix, cx + sin * inner + px, cy - cos * inner + py, color);
            WheelRenderer.vertex(buffer, matrix, cx + sin * inner - px, cy - cos * inner - py, color);
            WheelRenderer.vertex(buffer, matrix, cx + sin * outer - px, cy - cos * outer - py, color);
        }
        WheelRenderer.end(buffer);
    }

    public static void pointer(GuiGraphics graphics, float cx, float cy, float tipRadius, float baseRadius, float angleDeg, float halfWidth, int argb) {
        double angle = Math.toRadians(angleDeg);
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);
        float ux = sin;
        float uy = -cos;
        float tx = cos;
        float ty = sin;
        float baseX = cx + ux * baseRadius;
        float baseY = cy + uy * baseRadius;
        WheelRenderer.triangle(graphics, cx + ux * tipRadius, cy + uy * tipRadius, baseX + tx * halfWidth, baseY + ty * halfWidth, baseX - tx * halfWidth, baseY - ty * halfWidth, argb);
    }

    public static void triangle(GuiGraphics graphics, float x1, float y1, float x2, float y2, float x3, float y3, int argb) {
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = WheelRenderer.begin(graphics, VertexFormat.Mode.QUADS);
        WheelRenderer.vertex(buffer, matrix, x1, y1, argb);
        WheelRenderer.vertex(buffer, matrix, x2, y2, argb);
        WheelRenderer.vertex(buffer, matrix, x3, y3, argb);
        WheelRenderer.vertex(buffer, matrix, x3, y3, argb);
        WheelRenderer.end(buffer);
    }

    /** Decorative only — disabled for FPS (API kept). */
    public static void hexGrid(GuiGraphics graphics, int x1, int y1, int x2, int y2, float size, int argb) {
    }

    public static void roundedRect(GuiGraphics graphics, int x1, int y1, int x2, int y2, int radius, int topArgb, int bottomArgb) {
        int height = y2 - y1;
        int width = x2 - x1;
        if (height <= 0 || width <= 0) {
            return;
        }
        radius = Math.min(radius, Math.min(height / 2, width / 2));
        if (topArgb == bottomArgb && radius <= 0) {
            graphics.fill(x1, y1, x2, y2, topArgb);
            return;
        }
        int bands = Math.min(height, ROUND_BANDS);
        if (bands < 1) {
            bands = 1;
        }
        for (int i = 0; i < bands; ++i) {
            int ya = y1 + i * height / bands;
            int yb = y1 + (i + 1) * height / bands;
            if (yb <= ya) {
                continue;
            }
            int yMid = (ya + yb - 1) / 2;
            int fromEdge = Math.min(yMid - y1, y2 - 1 - yMid);
            int inset = 0;
            if (radius > 0 && fromEdge < radius) {
                int d = radius - 1 - fromEdge;
                inset = radius - (int) Math.round(Math.sqrt((double) radius * radius - (double) d * d));
                if (inset < 0) {
                    inset = 0;
                }
                if (inset > width / 2) {
                    inset = width / 2;
                }
            }
            float t = bands == 1 ? 0.0f : (float) i / (float) (bands - 1);
            graphics.fill(x1 + inset, ya, x2 - inset, yb, WheelRenderer.lerpColor(topArgb, bottomArgb, t));
        }
    }

    public static void roundedRect(GuiGraphics graphics, int x1, int y1, int x2, int y2, int radius, int argb) {
        WheelRenderer.roundedRect(graphics, x1, y1, x2, y2, radius, argb, argb);
    }

    public static void card(GuiGraphics graphics, int x1, int y1, int x2, int y2, int radius, int borderArgb, int topArgb, int bottomArgb) {
        WheelRenderer.roundedRect(graphics, x1, y1, x2, y2, radius, borderArgb);
        WheelRenderer.roundedRect(graphics, x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(1, radius - 1), topArgb, bottomArgb);
    }

    public static void neonPanel(GuiGraphics graphics, int x1, int y1, int x2, int y2, int radius, int neonArgb) {
        int glow = (neonArgb & 0x00FFFFFF) | 0x22000000;
        graphics.fill(x1 - 2, y1 - 2, x2 + 2, y2 + 2, glow);
        WheelRenderer.card(graphics, x1, y1, x2, y2, radius, neonArgb, UiTheme.BG_TOP, UiTheme.BG_BOTTOM);
        graphics.fill(x1 + radius, y1 + 1, x2 - radius, y1 + 2, (neonArgb & 0x00FFFFFF) | 0x66000000);
    }

    public static void neonSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 19, y + 19, UiTheme.SLOT_BORDER);
        graphics.fill(x, y, x + 18, y + 18, UiTheme.SLOT_BG);
        graphics.fill(x, y, x + 18, y + 1, UiTheme.GLOW_CYAN);
    }

    public static void neonBanner(GuiGraphics graphics, int x1, int y1, int x2, int y2, int neonArgb) {
        graphics.fill(x1, y1, x2, y2, 0xFF0A1220);
        graphics.fill(x1, y1, x2, y1 + 1, neonArgb);
        graphics.fill(x1, y2 - 1, x2, y2, neonArgb);
        graphics.fill(x1, y1, x1 + 1, y2, neonArgb);
        graphics.fill(x2 - 1, y1, x2, y2, neonArgb);
    }

    public static int chanceColor(float chance) {
        float t = Math.min(1.0f, (float) Math.sqrt(Math.max(0.0f, chance)));
        int r = (int) (232.0f + -156.0f * t);
        int g = (int) (59.0f + 158.0f * t);
        int b = (int) (59.0f + 41.0f * t);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    public static int lerpColor(int from, int to, float t) {
        if (from == to) {
            return from;
        }
        t = Math.max(0.0f, Math.min(1.0f, t));
        int a = WheelRenderer.lerpChannel(from >>> 24, to >>> 24, t);
        int r = WheelRenderer.lerpChannel(from >> 16 & 0xFF, to >> 16 & 0xFF, t);
        int g = WheelRenderer.lerpChannel(from >> 8 & 0xFF, to >> 8 & 0xFF, t);
        int b = WheelRenderer.lerpChannel(from & 0xFF, to & 0xFF, t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int lerpChannel(int from, int to, float t) {
        return (int) ((float) from + (float) (to - from) * t) & 0xFF;
    }

    private static BufferBuilder begin(GuiGraphics graphics, VertexFormat.Mode mode) {
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(mode, DefaultVertexFormat.POSITION_COLOR);
        return buffer;
    }

    private static void end(BufferBuilder buffer) {
        Tesselator.getInstance().end();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, int argb) {
        buffer.vertex(matrix, x, y, 0.0f).color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24).endVertex();
    }

    private WheelRenderer() {
    }
}
