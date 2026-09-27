package net.execheinz.upgrader.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Minimal 1.20-style GuiGraphics shim for Forge 1.19.2 (PoseStack-based).
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
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        GuiComponent.fill(this.pose, x1, y1, x2, y2, color);
    }

    public void fillGradient(int x1, int y1, int x2, int y2, int colorFrom, int colorTo) {
        int mid = (y1 + y2) / 2;
        GuiComponent.fill(this.pose, x1, y1, x2, mid, colorFrom);
        GuiComponent.fill(this.pose, x1, mid, x2, y2, colorTo);
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
        ItemRenderer items = this.minecraft.getItemRenderer();
        items.renderAndDecorateItem(stack, x, y);
    }

    public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
        this.minecraft.getItemRenderer().renderGuiItemDecorations(font, stack, x, y);
    }

    public void renderTooltip(Font font, ItemStack stack, int mouseX, int mouseY) {
        if (this.minecraft.screen == null) {
            return;
        }
        List<Component> lines = stack.getTooltipLines(
            this.minecraft.player,
            this.minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL
        );
        this.minecraft.screen.renderComponentTooltip(this.pose, lines, mouseX, mouseY);
    }

    public void renderComponentTooltip(Font font, List<Component> lines, int mouseX, int mouseY) {
        if (this.minecraft.screen == null) {
            return;
        }
        this.minecraft.screen.renderComponentTooltip(this.pose, lines, mouseX, mouseY);
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
