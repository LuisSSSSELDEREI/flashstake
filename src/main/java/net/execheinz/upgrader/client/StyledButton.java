package net.execheinz.upgrader.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Clean neon buttons — readable labels, soft hover, clear disabled state.
 */
public class StyledButton extends Button {
    private final int border;
    private final int top;
    private final int bottom;
    private final int textColor;
    private final int neon;

    public StyledButton(int x, int y, int width, int height, Component message, int border, int top, int bottom, int textColor, int chevronColor, int neon, Button.OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.border = border;
        this.top = top;
        this.bottom = bottom;
        this.textColor = textColor;
        this.neon = neon;
    }

    public static StyledButton gold(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
        return new StyledButton(x, y, width, height, message,
            UiTheme.NEON_GOLD, 0xFF3A2A08, 0xFF1E1504, UiTheme.NEON_GOLD, 0, UiTheme.NEON_GOLD, onPress);
    }

    public static StyledButton chip(int x, int y, int width, int height, Component message, int textColor, Button.OnPress onPress) {
        return new StyledButton(x, y, width, height, message,
            UiTheme.PANEL_BORDER, UiTheme.PANEL_TOP, UiTheme.PANEL_BOTTOM, textColor, 0, textColor, onPress);
    }

    public static StyledButton neon(int x, int y, int width, int height, Component message, int neonColor, Button.OnPress onPress) {
        return new StyledButton(x, y, width, height, message,
            neonColor, UiTheme.PANEL_TOP, UiTheme.PANEL_BOTTOM, neonColor, 0, neonColor, onPress);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean lit = this.active && this.isHovered();
        int x1 = this.getX();
        int y1 = this.getY();
        int x2 = x1 + this.width;
        int y2 = y1 + this.height;
        int radius = Math.min(5, Math.max(3, this.height / 4));

        if (this.active && this.neon != 0) {
            int glowA = lit ? 0x40 : 0x22;
            int glow = (this.neon & 0x00FFFFFF) | (glowA << 24);
            WheelRenderer.roundedRect(graphics, x1 - 1, y1 - 1, x2 + 1, y2 + 1, radius + 1, glow);
        }

        int borderColor;
        int topColor;
        int bottomColor;
        if (!this.active) {
            borderColor = 0xFF2A3040;
            topColor = 0xFF141820;
            bottomColor = 0xFF0C1018;
        } else if (lit) {
            borderColor = WheelRenderer.lerpColor(this.border, 0xFFFFFFFF, 0.28f);
            topColor = WheelRenderer.lerpColor(this.top, 0xFFFFFFFF, 0.10f);
            bottomColor = WheelRenderer.lerpColor(this.bottom, 0xFFFFFFFF, 0.06f);
        } else {
            borderColor = this.border;
            topColor = this.top;
            bottomColor = this.bottom;
        }
        WheelRenderer.card(graphics, x1, y1, x2, y2, radius, borderColor, topColor, bottomColor);

        // Soft top highlight
        if (this.active) {
            int hi = (this.neon != 0 ? this.neon : UiTheme.TEXT) & 0x00FFFFFF;
            graphics.fill(x1 + 3, y1 + 1, x2 - 3, y1 + 2, hi | 0x55000000);
        }

        Font font = Minecraft.getInstance().font;
        int labelColor = this.active ? this.textColor : UiTheme.TEXT_MUTED;
        int pad = 4;
        String label = this.getMessage().getString();
        int maxW = Math.max(4, this.width - pad * 2);
        // Prefer full text: shrink slightly before ellipsizing
        float scale = 1.0f;
        if (font.width(label) > maxW) {
            scale = Math.max(0.65f, maxW / (float) Math.max(1, font.width(label)));
        }
        if (font.width(label) * scale > maxW + 0.5f) {
            label = UiText.ellipsize(font, label, maxW);
            scale = 1.0f;
        }
        int textWidth = Math.round(font.width(label) * scale);
        int textX = x1 + (this.width - textWidth) / 2;
        int textY = y1 + (this.height - Math.round(9 * scale)) / 2;
        if (scale < 0.999f) {
            graphics.pose().pushPose();
            graphics.pose().translate(textX, textY, 0);
            graphics.pose().scale(scale, scale, 1.0f);
            if (this.active) {
                graphics.drawString(font, label, 1, 1, 0x66000000, false);
            }
            graphics.drawString(font, label, 0, 0, labelColor, false);
            graphics.pose().popPose();
        } else {
            if (this.active) {
                graphics.drawString(font, label, textX + 1, textY + 1, 0x66000000, false);
            }
            graphics.drawString(font, label, textX, textY, labelColor, false);
        }
    }

    @Override
    public void onPress() {
        if (this.active) {
            FlashFx.softClick();
        }
        super.onPress();
    }
}
