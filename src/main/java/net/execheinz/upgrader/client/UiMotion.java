package net.execheinz.upgrader.client;

import net.minecraft.client.gui.Font;
import net.execheinz.upgrader.client.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Lightweight motion overlays — fixed-size pools, no particle spam.
 */
public final class UiMotion {
    private static final int FLOAT_CAP = 8;
    private static final Floater[] FLOATERS = new Floater[FLOAT_CAP];

    private static long displayBalance;
    private static long targetBalance;
    private static boolean balanceInit;
    private static long flashUntilMs;
    private static int flashRgb = UiTheme.NEON_LIME;
    private static long panelPulseMs;

    static {
        for (int i = 0; i < FLOAT_CAP; ++i) {
            FLOATERS[i] = new Floater();
        }
    }

    private UiMotion() {
    }

    public static void onBalance(long balance) {
        if (!balanceInit) {
            displayBalance = balance;
            targetBalance = balance;
            balanceInit = true;
            return;
        }
        long prev = targetBalance;
        targetBalance = balance;
        long delta = balance - prev;
        if (delta > 0L) {
            flashUntilMs = System.currentTimeMillis() + 420L;
            flashRgb = UiTheme.NEON_LIME;
            FlashFx.coin();
            pushFloater("+" + delta, UiTheme.NEON_LIME);
        } else if (delta < 0L) {
            flashUntilMs = System.currentTimeMillis() + 320L;
            flashRgb = UiTheme.NEON_MAGENTA;
            FlashFx.spend();
            pushFloater(Long.toString(delta), UiTheme.NEON_MAGENTA);
        }
    }

    public static void resetBalance() {
        balanceInit = false;
        displayBalance = 0L;
        targetBalance = 0L;
    }

    public static long smoothBalance() {
        long diff = targetBalance - displayBalance;
        if (diff == 0L) {
            return displayBalance;
        }
        long step = Math.max(1L, Math.abs(diff) / 8L);
        if (Math.abs(diff) <= step) {
            displayBalance = targetBalance;
        } else {
            displayBalance += diff > 0L ? step : -step;
        }
        return displayBalance;
    }

    public static void pulsePanel() {
        panelPulseMs = System.currentTimeMillis();
        FlashFx.whoosh();
    }

    public static void celebrateWin(boolean big) {
        panelPulseMs = System.currentTimeMillis();
        if (big) {
            FlashFx.bigWin();
            pushFloater("WIN!", UiTheme.NEON_GOLD);
        } else {
            FlashFx.win();
            pushFloater("WIN", UiTheme.NEON_LIME);
        }
    }

    public static void celebrateLose() {
        panelPulseMs = System.currentTimeMillis();
        FlashFx.lose();
        pushFloater("LOSE", UiTheme.NEON_MAGENTA);
    }

    public static void pushFloater(String text, int rgb) {
        long oldest = Long.MAX_VALUE;
        int slot = 0;
        for (int i = 0; i < FLOAT_CAP; ++i) {
            Floater f = FLOATERS[i];
            if (!f.active) {
                slot = i;
                break;
            }
            if (f.startMs < oldest) {
                oldest = f.startMs;
                slot = i;
            }
        }
        Floater f = FLOATERS[slot];
        f.active = true;
        f.text = text;
        f.rgb = rgb;
        f.startMs = System.currentTimeMillis();
        f.durationMs = 900L;
        f.xOff = (slot % 4) * 18 - 24;
    }

    public static void renderBalanceFlash(GuiGraphics g, Font font, int x, int y, String prefix) {
        long bal = smoothBalance();
        long now = System.currentTimeMillis();
        float pulse = now < flashUntilMs ? FlashFx.pulse(flashUntilMs - 420L, 420L) : 0.0f;
        int color = pulse > 0.05f ? FlashFx.withAlpha(flashRgb, 0.55f + 0.45f * pulse) : UiTheme.TEXT;
        g.drawString(font, prefix + bal, x, y, color, false);
        renderFloaters(g, font, x + 70, y);
    }

    public static void renderPanelPulse(GuiGraphics g, int x1, int y1, int x2, int y2, int rgb) {
        float p = FlashFx.pulse(panelPulseMs, 480L);
        if (p <= 0.02f) {
            return;
        }
        int glow = FlashFx.withAlpha(rgb, 0.12f + 0.28f * p);
        int pad = Math.round(2.0f + 4.0f * p);
        WheelRenderer.roundedRect(g, x1 - pad, y1 - pad, x2 + pad, y2 + pad, 8, glow);
    }

    public static void renderFloaters(GuiGraphics g, Font font, int originX, int originY) {
        long now = System.currentTimeMillis();
        for (Floater f : FLOATERS) {
            if (!f.active) {
                continue;
            }
            float t = (now - f.startMs) / (float) f.durationMs;
            if (t >= 1.0f) {
                f.active = false;
                continue;
            }
            float ease = FlashFx.easeOutCubic(t);
            int y = originY - Math.round(ease * 18.0f);
            int x = originX + f.xOff;
            float alpha = t < 0.2f ? t / 0.2f : 1.0f - (t - 0.2f) / 0.8f;
            g.drawString(font, f.text, x, y, FlashFx.withAlpha(f.rgb, Mth.clamp(alpha, 0.0f, 1.0f)), false);
        }
    }

    private static final class Floater {
        boolean active;
        String text = "";
        int rgb;
        long startMs;
        long durationMs;
        int xOff;
    }
}
