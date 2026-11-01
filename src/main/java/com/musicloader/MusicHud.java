package com.musicloader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Рисует твой текст прямо в игре (справа внизу), когда меню не открыто. */
public final class MusicHud {
    private MusicHud() {}

    public static void render(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        String text = MusicLibrary.cachedText();
        if (text.isEmpty()) return;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        g.text(mc.font, text, w - mc.font.width(text) - 8, h - 14, 0xFFFFFFFF, true);
    }
}
