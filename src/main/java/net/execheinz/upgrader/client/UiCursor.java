package net.execheinz.upgrader.client;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
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

    private static boolean fieldsResolved;
    private static Field xposField;
    private static Field yposField;

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
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getWindow() == null || mc.mouseHandler == null) {
                return;
            }
            long window = mc.getWindow().getWindow();
            GLFW.glfwSetCursorPos(window, savedX, savedY);
            writeMouseHandler(mc.mouseHandler, savedX, savedY);
        } catch (Throwable ignored) {
            // Never crash the client over cursor restore — wrong SRG names used to NPE/IAE here.
        }
    }

    private static boolean isFlashStakeScreen(Screen screen) {
        return screen instanceof UpgraderScreen
            || screen instanceof MarketScreen
            || screen instanceof CasesScreen
            || screen instanceof DoubleScreen
            || screen instanceof ArenaScreen;
    }

    private static void writeMouseHandler(MouseHandler handler, double x, double y) {
        resolveFields(handler);
        if (xposField == null || yposField == null) {
            return;
        }
        try {
            xposField.setDouble(handler, x);
            yposField.setDouble(handler, y);
        } catch (Throwable ignored) {
        }
    }

    private static void resolveFields(MouseHandler handler) {
        if (fieldsResolved) {
            return;
        }
        fieldsResolved = true;
        Class<?> cls = handler.getClass();

        // Forge 1.20.5+ ships Mojang mappings at runtime.
        xposField = findDoubleField(cls, "xpos");
        yposField = findDoubleField(cls, "ypos");
        if (xposField != null && yposField != null) {
            return;
        }

        // Older Forge (1.19.2–1.20.4) uses SRG names; do not hardcode f_* —
        // wrong IDs (e.g. boolean grab flag) crash with IllegalArgumentException.
        // MouseHandler declares xpos then ypos as the first two double fields.
        List<Field> doubles = new ArrayList<>();
        for (Field field : cls.getDeclaredFields()) {
            if (field.getType() == double.class) {
                field.setAccessible(true);
                doubles.add(field);
            }
        }
        if (doubles.size() >= 2) {
            xposField = doubles.get(0);
            yposField = doubles.get(1);
        }
    }

    private static Field findDoubleField(Class<?> cls, String name) {
        try {
            Field field = cls.getDeclaredField(name);
            if (field.getType() != double.class) {
                return null;
            }
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
