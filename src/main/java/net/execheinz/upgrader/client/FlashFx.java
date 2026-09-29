package net.execheinz.upgrader.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * Cheap UI audio — vanilla sounds only, no custom assets, no allocations in hot paths.
 */
public final class FlashFx {
    private static long lastTickMs;
    private static long lastClickMs;

    private FlashFx() {
    }

    public static void click() {
        long now = System.currentTimeMillis();
        if (now - lastClickMs < 40L) {
            return;
        }
        lastClickMs = now;
        play(SoundEvents.UI_BUTTON_CLICK, 1.05f, 0.35f);
    }

    public static void softClick() {
        play(SoundEvents.UI_BUTTON_CLICK, 1.35f, 0.22f);
    }

    public static void coin() {
        play(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.6f, 0.28f);
    }

    public static void spend() {
        play(SoundEvents.NOTE_BLOCK_BASS, 0.85f, 0.35f);
    }

    public static void win() {
        play(SoundEvents.PLAYER_LEVELUP, 1.35f, 0.45f);
    }

    public static void bigWin() {
        play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.1f, 0.55f);
    }

    public static void lose() {
        play(SoundEvents.ANVIL_LAND, 0.75f, 0.28f);
    }

    public static void rare() {
        play(SoundEvents.PLAYER_LEVELUP, 1.7f, 0.4f);
    }

    public static void tick(float pitch) {
        long now = System.currentTimeMillis();
        if (now - lastTickMs < 35L) {
            return;
        }
        lastTickMs = now;
        play(SoundEvents.NOTE_BLOCK_HAT, pitch, 0.18f);
    }

    public static void whoosh() {
        play(SoundEvents.NOTE_BLOCK_CHIME, 1.4f, 0.25f);
    }

    public static void confirm() {
        play(SoundEvents.NOTE_BLOCK_PLING, 1.5f, 0.35f);
    }

    public static void play(SoundEvent sound, float pitch, float volume) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || sound == null) {
            return;
        }
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5f, 2.0f), Mth.clamp(volume, 0.05f, 1.0f)));
    }

    /** 0→1→0 pulse over durationMs. */
    public static float pulse(long startMs, long durationMs) {
        if (durationMs <= 0L) {
            return 0.0f;
        }
        float t = (System.currentTimeMillis() - startMs) / (float) durationMs;
        if (t <= 0.0f || t >= 1.0f) {
            return 0.0f;
        }
        return (float) Math.sin(t * Math.PI);
    }

    public static float easeOutCubic(float t) {
        t = Mth.clamp(t, 0.0f, 1.0f);
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv;
    }

    public static int withAlpha(int rgb, float alpha) {
        int a = Mth.clamp(Math.round(alpha * 255.0f), 0, 255);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
