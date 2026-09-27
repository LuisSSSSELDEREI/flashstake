/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.renderer.GameRenderer
 *  org.joml.Matrix4f
 */
package net.execheinz.upgrader.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public final class WheelRenderer {
    public static void arc(GuiGraphics graphics, float cx, float cy, float innerRadius, float outerRadius, float startDeg, float sweepDeg, int argb) {
        WheelRenderer.arc(graphics, cx, cy, innerRadius, outerRadius, startDeg, sweepDeg, argb, argb);
    }

    public static void arc(GuiGraphics graphics, float cx, float cy, float innerRadius, float outerRadius, float startDeg, float sweepDeg, int fromArgb, int toArgb) {
        if (sweepDeg <= 0.0f) {
            return;
        }
        sweepDeg = Math.min(sweepDeg, 360.0f);
        int segments = Math.max(2, Math.round(sweepDeg / 2.0f));
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = WheelRenderer.begin(graphics, VertexFormat.Mode.TRIANGLE_STRIP);
        for (int i = 0; i <= segments; ++i) {
            float t = (float)i / (float)segments;
            int color = WheelRenderer.lerpColor(fromArgb, toArgb, t);
            double angle = Math.toRadians(startDeg + sweepDeg * t);
            float sin = (float)Math.sin(angle);
            float cos = (float)Math.cos(angle);
            WheelRenderer.vertex(buffer, matrix, cx + sin * outerRadius, cy - cos * outerRadius, color);
            WheelRenderer.vertex(buffer, matrix, cx + sin * innerRadius, cy - cos * innerRadius, color);
        }
        WheelRenderer.end(buffer);
    }

    public static void disc(GuiGraphics graphics, float cx, float cy, float radius, int argb) {
        WheelRenderer.arc(graphics, cx, cy, 0.0f, radius, 0.0f, 360.0f, argb);
    }

    public static void tickRing(GuiGraphics graphics, float cx, float cy, float innerRadius, float outerRadius, float rotationDeg, int count, int majorEvery, int minorArgb, int majorArgb) {
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = WheelRenderer.begin(graphics, VertexFormat.Mode.QUADS);
        for (int i = 0; i < count; ++i) {
            boolean major = i % majorEvery == 0;
            int color = major ? majorArgb : minorArgb;
            float inner = major ? innerRadius - 1.5f : innerRadius;
            float outer = major ? outerRadius + 1.0f : outerRadius;
            float half = major ? 0.9f : 0.5f;
            double angle = Math.toRadians(rotationDeg + 360.0f * (float)i / (float)count);
            float sin = (float)Math.sin(angle);
            float cos = (float)Math.cos(angle);
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
        float sin = (float)Math.sin(angle);
        float cos = (float)Math.cos(angle);
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

    public static void hexGrid(GuiGraphics graphics, int x1, int y1, int x2, int y2, float size, int argb) {
        graphics.enableScissor(x1, y1, x2, y2);
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = WheelRenderer.begin(graphics, VertexFormat.Mode.QUADS);
        float stepX = size * 1.5f;
        float stepY = (float)(Math.sqrt(3.0) * (double)size);
        int cols = (int)Math.ceil((float)(x2 - x1) / stepX) + 2;
        int rows = (int)Math.ceil((float)(y2 - y1) / stepY) + 2;
        for (int col = -1; col < cols; ++col) {
            for (int row = -1; row < rows; ++row) {
                float cx = (float)x1 + (float)col * stepX;
                float cy = (float)y1 + (float)row * stepY + (col % 2 == 0 ? 0.0f : stepY * 0.5f);
                for (int edge = 0; edge < 6; ++edge) {
                    double a1 = Math.toRadians(60.0 * (double)edge);
                    double a2 = Math.toRadians(60.0 * (double)(edge + 1));
                    WheelRenderer.segment(buffer, matrix, cx + (float)Math.cos(a1) * size, cy + (float)Math.sin(a1) * size, cx + (float)Math.cos(a2) * size, cy + (float)Math.sin(a2) * size, 0.5f, argb);
                }
            }
        }
        WheelRenderer.end(buffer);
        graphics.disableScissor();
    }

    public static void roundedRect(GuiGraphics graphics, int x1, int y1, int x2, int y2, int radius, int topArgb, int bottomArgb) {
        int height = y2 - y1;
        if (height <= 0 || x2 <= x1) {
            return;
        }
        radius = Math.min(radius, Math.min(height / 2, (x2 - x1) / 2));
        for (int y = y1; y < y2; ++y) {
            int fromEdge = Math.min(y - y1, y2 - 1 - y);
            int inset = fromEdge >= radius ? 0 : radius - (int)Math.round(Math.sqrt(radius * radius - (radius - 1 - fromEdge) * (radius - 1 - fromEdge)));
            int color = WheelRenderer.lerpColor(topArgb, bottomArgb, (float)(y - y1) / (float)Math.max(1, height - 1));
            graphics.fill(x1 + inset, y, x2 - inset, y + 1, color);
        }
    }

    public static void roundedRect(GuiGraphics graphics, int x1, int y1, int x2, int y2, int radius, int argb) {
        WheelRenderer.roundedRect(graphics, x1, y1, x2, y2, radius, argb, argb);
    }

    public static void card(GuiGraphics graphics, int x1, int y1, int x2, int y2, int radius, int borderArgb, int topArgb, int bottomArgb) {
        WheelRenderer.roundedRect(graphics, x1, y1, x2, y2, radius, borderArgb);
        WheelRenderer.roundedRect(graphics, x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(1, radius - 1), topArgb, bottomArgb);
    }

    /** Soft neon outer glow + dark panel. */
    public static void neonPanel(GuiGraphics graphics, int x1, int y1, int x2, int y2, int radius, int neonArgb) {
        int glow = (neonArgb & 0x00FFFFFF) | 0x28000000;
        WheelRenderer.roundedRect(graphics, x1 - 2, y1 - 2, x2 + 2, y2 + 2, radius + 2, glow);
        WheelRenderer.card(graphics, x1, y1, x2, y2, radius, neonArgb, UiTheme.BG_TOP, UiTheme.BG_BOTTOM);
        // top highlight line
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
        float t = Math.min(1.0f, (float)Math.sqrt(Math.max(0.0f, chance)));
        int r = (int)(232.0f + -156.0f * t);
        int g = (int)(59.0f + 158.0f * t);
        int b = (int)(59.0f + 41.0f * t);
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
        return (int)((float)from + (float)(to - from) * t) & 0xFF;
    }

    private static BufferBuilder begin(GuiGraphics graphics, VertexFormat.Mode mode) {
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        return Tesselator.getInstance().begin(mode, DefaultVertexFormat.POSITION_COLOR);
    }

    private static void end(BufferBuilder buffer) {
        MeshData mesh = buffer.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void segment(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float x2, float y2, float halfWidth, int argb) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float)Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-4f) {
            return;
        }
        float nx = -dy / length * halfWidth;
        float ny = dx / length * halfWidth;
        WheelRenderer.vertex(buffer, matrix, x1 + nx, y1 + ny, argb);
        WheelRenderer.vertex(buffer, matrix, x2 + nx, y2 + ny, argb);
        WheelRenderer.vertex(buffer, matrix, x2 - nx, y2 - ny, argb);
        WheelRenderer.vertex(buffer, matrix, x1 - nx, y1 - ny, argb);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, int argb) {
        buffer.addVertex(matrix, x, y, 0.0f).setColor(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24);
    }

    private WheelRenderer() {
    }
}
