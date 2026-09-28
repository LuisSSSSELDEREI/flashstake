package net.execheinz.upgrader.client;

import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

/**
 * Keeps the mouse where it was when switching between FlashStake menus.
 * Vanilla often recenters the cursor when one container screen replaces another.
 */
public final class UiCursor {
    private static boolean pending;
    private static double savedX;
    private static double savedY;
    private static int framesLeft;

    private UiCursor() {
    }

    /** Call right before sending an Open* packet while already in a FlashStake GUI. */
    public static void captureIfInFlashStakeUi() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.mouseHandler == null) {
            return;
        }
        Screen screen = mc.screen;
        if (!isFlashStakeScreen(screen)) {
            return;
        }
        savedX = mc.mouseHandler.xpos();
        savedY = mc.mouseHandler.ypos();
        pending = true;
        framesLeft = 0;
    }

    /** Call from Screen.init — restore runs after setScreen/releaseMouse finishes. */
    public static void scheduleRestore() {
        if (!pending) {
            return;
        }
        framesLeft = 5;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.execute(UiCursor::apply);
        }
    }

    /** Call at the start of Screen.render for a couple frames (covers late recenter). */
    public static void tickInRender() {
        if (!pending || framesLeft <= 0) {
            return;
        }
        apply();
        framesLeft--;
        if (framesLeft <= 0) {
            pending = false;
        }
    }

    private static void apply() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null || mc.mouseHandler == null) {
            return;
        }
        long window = mc.getWindow().getWindow();
        GLFW.glfwSetCursorPos(window, savedX, savedY);
        writeMouseHandler(mc.mouseHandler, savedX, savedY);
    }

    private static boolean isFlashStakeScreen(Screen screen) {
        return screen instanceof UpgraderScreen
            || screen instanceof MarketScreen
            || screen instanceof CasesScreen
            || screen instanceof DoubleScreen;
    }

    private static void writeMouseHandler(MouseHandler handler, double x, double y) {
        // Forge 1.20.5+ ships Mojang mappings at runtime.
        if (setDoubleField(handler, "xpos", x) && setDoubleField(handler, "ypos", y)) {
            return;
        }
        // Fallback SRG names used by some tooling
        setDoubleField(handler, "f_91520_", x);
        setDoubleField(handler, "f_91521_", y);
    }

    private static boolean setDoubleField(Object target, String name, double value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.setDouble(target, value);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
