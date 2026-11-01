package com.musicloader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Открывает экран. В новых версиях Minecraft метод переименовывали, поэтому ищем его по имени. */
public final class ScreenUtil {
    private ScreenUtil() {}

    public static void open(Screen screen) {
        Minecraft mc = Minecraft.getInstance();
        try {
            Method m = Minecraft.class.getMethod("setScreenAndShow", Screen.class);
            m.invoke(mc, screen);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method m = Minecraft.class.getMethod("setScreen", Screen.class);
            m.invoke(mc, screen);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Field f = Minecraft.class.getField("gui");
            Object gui = f.get(mc);
            Method m = gui.getClass().getMethod("setScreen", Screen.class);
            m.invoke(gui, screen);
        } catch (ReflectiveOperationException e) {
            MusicLoaderClient.LOGGER.error("Не удалось открыть экран", e);
        }
    }
}
