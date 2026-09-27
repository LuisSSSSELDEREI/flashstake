package net.execheinz.upgrader.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public class StyledButton extends Button {
    private final int border;
    private final int top;
    private final int bottom;
    private final int textColor;
    private final int chevronColor;
    private final int neon;

    public StyledButton(int x, int y, int width, int height, Component message, int border, int top, int bottom, int textColor, int chevronColor, int neon, Button.OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.border = border;
        this.top = top;
        this.bottom = bottom;
        this.textColor = textColor;
        this.chevronColor = chevronColor;
        this.neon = neon;
    }

    public static StyledButton gold(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
        return new StyledButton(x, y, width, height, message,
            UiTheme.NEON_GOLD, 0xFF3A2A08, 0xFF1E1504, UiTheme.NEON_GOLD, UiTheme.NEON_GOLD, UiTheme.NEON_GOLD, onPress);
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

        if (this.active && this.neon != 0) {
            int glow = (this.neon & 0x00FFFFFF) | (lit ? 0x55000000 : 0x28000000);
            WheelRenderer.roundedRect(graphics, x1 - 1, y1 - 1, x2 + 1, y2 + 1, 5, glow);
        }

        int borderColor = this.active ? (lit ? WheelRenderer.lerpColor(this.border, -1, 0.35f) : this.border) : 0xFF2A3040;
        int topColor = this.active ? (lit ? WheelRenderer.lerpColor(this.top, -1, 0.12f) : this.top) : 0xFF12151C;
        int bottomColor = this.active ? (lit ? WheelRenderer.lerpColor(this.bottom, -1, 0.12f) : this.bottom) : 0xFF0A0C12;
        WheelRenderer.card(graphics, x1, y1, x2, y2, 4, borderColor, topColor, bottomColor);

        if (this.active && this.neon != 0) {
            graphics.fill(x1 + 3, y1 + 1, x2 - 3, y1 + 2, (this.neon & 0x00FFFFFF) | 0x88000000);
        }

        Font font = Minecraft.getInstance().font;
        int labelColor = this.active ? this.textColor : UiTheme.TEXT_MUTED;
        int textWidth = font.width((FormattedText) this.getMessage());
        boolean chevron = this.chevronColor != 0;
        int groupWidth = textWidth + (chevron ? 12 : 0);
        int groupX = x1 + (this.width - groupWidth) / 2;
        // Font glyph height is 9 — center cleanly in the button
        int textY = y1 + (this.height - 9) / 2;
        int textX = chevron ? groupX + 12 : groupX;
        if (chevron) {
            drawChevron(graphics, groupX, textY + 1, this.active ? this.chevronColor : UiTheme.TEXT_MUTED);
        }
        graphics.drawString(font, this.getMessage(), textX, textY, labelColor, false);
    }

    private static void drawChevron(GuiGraphics graphics, int x, int y, int color) {
        for (int row = 0; row < 3; ++row) {
            graphics.fill(x + 3 - row, y + row, x + 4 - row, y + row + 1, color);
            graphics.fill(x + 3 + row, y + row, x + 4 + row, y + row + 1, color);
            graphics.fill(x + 3 - row, y + row + 3, x + 4 - row, y + row + 4, color);
            graphics.fill(x + 3 + row, y + row + 3, x + 4 + row, y + row + 4, color);
        }
    }
}
