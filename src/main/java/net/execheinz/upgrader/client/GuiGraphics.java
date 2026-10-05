package net.execheinz.upgrader.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Minimal 1.20-style GuiGraphics shim for 1.19.2 (PoseStack-based).
 */
public final class GuiGraphics {
    private final Minecraft minecraft;
    private final PoseStack pose;

    public GuiGraphics(Minecraft minecraft, PoseStack pose) {
        this.minecraft = minecraft;
        this.pose = pose;
    }

    public static GuiGraphics of(PoseStack pose) {
        return new GuiGraphics(Minecraft.getInstance(), pose);
    }

    public PoseStack pose() {
        return this.pose;
    }

    public void flush() {
        this.resetGuiState();
    }

    /** Clear leftover item-renderer GL state so text/tooltips don't sample the item atlas. */
    private void resetGuiState() {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Lighting.setupForFlatItems();
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        GuiComponent.fill(this.pose, x1, y1, x2, y2, color);
    }

    public void fillGradient(int x1, int y1, int x2, int y2, int colorFrom, int colorTo) {
        int h = y2 - y1;
        if (h <= 0 || x2 <= x1) {
            return;
        }
        // Stepped blend — old 2-band fill made a hard black bar on case tiles.
        int steps = Math.max(1, Math.min(h, 24));
        for (int i = 0; i < steps; ++i) {
            int ya = y1 + h * i / steps;
            int yb = y1 + h * (i + 1) / steps;
            if (yb <= ya) {
                continue;
            }
            float t = (i + 0.5f) / steps;
            GuiComponent.fill(this.pose, x1, ya, x2, yb, lerpArgb(colorFrom, colorTo, t));
        }
    }

    private static int lerpArgb(int from, int to, float t) {
        int a = (int) (((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = (int) (((from >>> 16) & 0xFF) + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * t);
        int g = (int) (((from >>> 8) & 0xFF) + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * t);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawShadow(this.pose, text, (float) x, (float) y, color);
        } else {
            font.draw(this.pose, text, (float) x, (float) y, color);
        }
    }

    public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawShadow(this.pose, text, (float) x, (float) y, color);
        } else {
            font.draw(this.pose, text, (float) x, (float) y, color);
        }
    }

    public void drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawShadow(this.pose, text, (float) x, (float) y, color);
        } else {
            font.draw(this.pose, text, (float) x, (float) y, color);
        }
    }

    public void drawCenteredString(Font font, Component text, int x, int y, int color) {
        int w = font.width((FormattedText) text);
        this.drawString(font, text, x - w / 2, y, color, false);
    }

    public void drawCenteredString(Font font, String text, int x, int y, int color) {
        int w = font.width(text);
        this.drawString(font, text, x - w / 2, y, color, false);
    }

    public void drawWordWrap(Font font, FormattedText text, int x, int y, int width, int color) {
        for (FormattedCharSequence line : font.split(text, width)) {
            this.drawString(font, line, x, y, color, false);
            y += 9;
        }
    }

    public void renderItem(ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        // ItemRenderer GUI blit ignores PoseStack — push our matrix onto the model-view
        // so translate/rotate/scale (sword needle, case reels) actually apply.
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.mulPoseMatrix(this.pose.last().pose());
        RenderSystem.applyModelViewMatrix();
        try {
            this.minecraft.getItemRenderer().renderGuiItem(stack, x, y);
        } finally {
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            this.resetGuiState();
        }
    }

    public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.mulPoseMatrix(this.pose.last().pose());
        RenderSystem.applyModelViewMatrix();
        try {
            this.minecraft.getItemRenderer().renderGuiItemDecorations(font, stack, x, y);
        } finally {
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            this.resetGuiState();
        }
    }

    public void renderTooltip(Font font, ItemStack stack, int mouseX, int mouseY) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        List<Component> lines = stack.getTooltipLines(
            this.minecraft.player,
            this.minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL
        );
        this.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    /**
     * Draw tooltips ourselves — Screen.renderComponentTooltip often samples the item atlas
     * after custom item draws on 1.19.2, producing a magenta square over the text.
     */
    public void renderComponentTooltip(Font font, List<Component> lines, int mouseX, int mouseY) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        this.disableScissor();
        this.resetGuiState();

        int maxW = 0;
        for (Component line : lines) {
            maxW = Math.max(maxW, font.width(line));
        }
        int lineH = 10;
        int gapAfterFirst = lines.size() > 1 ? 2 : 0;
        int boxH = 8 + lines.size() * lineH + gapAfterFirst;

        int screenW = this.minecraft.getWindow().getGuiScaledWidth();
        int screenH = this.minecraft.getWindow().getGuiScaledHeight();
        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + maxW + 8 > screenW) {
            x = mouseX - 16 - maxW;
        }
        if (y + boxH > screenH) {
            y = screenH - boxH - 4;
        }
        if (x < 4) {
            x = 4;
        }
        if (y < 4) {
            y = 4;
        }

        int x1 = x - 4;
        int y1 = y - 4;
        int x2 = x + maxW + 4;
        int y2 = y + boxH - 4;
        // Draw above items / decorations that may share the same pose stack
        this.pose.pushPose();
        this.pose.translate(0.0f, 0.0f, 400.0f);
        // Vanilla-like dark tooltip panel + purple border
        GuiComponent.fill(this.pose, x1, y1, x2, y2, 0xF0100010);
        GuiComponent.fill(this.pose, x1, y1 - 1, x2, y1, 0x505000FF);
        GuiComponent.fill(this.pose, x1, y2, x2, y2 + 1, 0x5028007F);
        GuiComponent.fill(this.pose, x1 - 1, y1, x1, y2, 0x505000FF);
        GuiComponent.fill(this.pose, x2, y1, x2 + 1, y2, 0x5028007F);

        int ty = y;
        for (int i = 0; i < lines.size(); ++i) {
            this.drawString(font, lines.get(i), x, ty, 0xFFFFFF, false);
            ty += lineH;
            if (i == 0) {
                ty += gapAfterFirst;
            }
        }
        this.pose.popPose();
        this.resetGuiState();
    }

    public void blit(ResourceLocation texture, int x, int y, int u, int v, int width, int height, int texW, int texH) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        GuiComponent.blit(this.pose, x, y, (float) u, (float) v, width, height, texW, texH);
    }

    public void blit(ResourceLocation texture, int x, int y, int width, int height, float u, float v, int regionW, int regionH, int texW, int texH) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        GuiComponent.blit(this.pose, x, y, width, height, u, v, regionW, regionH, texW, texH);
    }

    public void enableScissor(int x1, int y1, int x2, int y2) {
        Window window = this.minecraft.getWindow();
        int scale = (int) window.getGuiScale();
        int sx = x1 * scale;
        int sy = window.getHeight() - y2 * scale;
        int sw = (x2 - x1) * scale;
        int sh = (y2 - y1) * scale;
        RenderSystem.enableScissor(sx, Math.max(0, sy), Math.max(0, sw), Math.max(0, sh));
    }

    public void disableScissor() {
        RenderSystem.disableScissor();
    }
}
