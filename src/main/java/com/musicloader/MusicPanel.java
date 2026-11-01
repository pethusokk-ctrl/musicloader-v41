package com.musicloader;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Музыкальная панель на экране паузы: плавно выезжает слева, всё нарисовано вручную. */
public final class MusicPanel {
    private static final Map<Path, Identifier> ICONS = new HashMap<>();
    private static final Map<Path, Boolean> ICON_FAILED = new HashMap<>();

    private static final int ACCENT = 0x7FD400;

    private MusicPanel() {}

    public static void build(Screen screen, Consumer<AbstractWidget> add) {
        add.accept(new PanelWidget(screen, screen.width, screen.height));
    }

    // ---------- вспомогательное рисование ----------

    private static int argb(int rgb, float a) {
        int al = (int) (Math.max(0f, Math.min(1f, a)) * 255f);
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    private static void circle(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        double rr = r + 0.5;
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.round(Math.sqrt(rr * rr - (double) dy * dy));
            g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
        }
    }

    private static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, int border) {
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    private static void text(GuiGraphicsExtractor g, Font font, String s, int x, int y, int rgb, float a) {
        if (a < 0.08f) return;
        g.text(font, s, x, y, argb(rgb, a), true);
    }

    private static void centered(GuiGraphicsExtractor g, Font font, String s, int cx, int y, int rgb, float a) {
        text(g, font, s, cx - font.width(s) / 2, y, rgb, a);
    }

    private static String fit(Font font, String s, int maxW) {
        if (font.width(s) <= maxW) return s;
        while (s.length() > 1 && font.width(s + "...") > maxW) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    private static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static boolean overCircle(int mx, int my, int cx, int cy, int r) {
        int dx = mx - cx, dy = my - cy;
        return dx * dx + dy * dy <= r * r;
    }

    private static Identifier iconFor(Path path) {
        if (path == null || ICON_FAILED.containsKey(path)) return null;
        Identifier cached = ICONS.get(path);
        if (cached != null) return cached;
        try (InputStream in = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(in);
            Identifier id = Identifier.fromNamespaceAndPath(MusicLoaderClient.MOD_ID,
                    "icon/" + Integer.toHexString(path.toString().hashCode()));
            Minecraft.getInstance().getTextureManager().register(id,
                    new DynamicTexture(() -> "musicloader icon", image));
            ICONS.put(path, id);
            return id;
        } catch (Exception e) {
            MusicLoaderClient.LOGGER.warn("Иконка не загрузилась: {}", path);
            ICON_FAILED.put(path, true);
            return null;
        }
    }

    private static String time(int sec) {
        return (sec / 60) + ":" + String.format("%02d", sec % 60);
    }

    // ---------- сама панель ----------

    private static final class PanelWidget extends AbstractWidget {
        private final Screen screen;
        private final int sw, sh;
        private final long start = System.currentTimeMillis();
        private boolean wasDown = Minecraft.getInstance().mouseHandler.isLeftPressed();
        private boolean dragging = false;

        PanelWidget(Screen screen, int sw, int sh) {
            super(0, 0, 0, 0, Component.empty()); // нулевая область - сам клики не перехватывает
            this.screen = screen;
            this.sw = sw;
            this.sh = sh;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            Minecraft mc = Minecraft.getInstance();
            Font font = mc.font;

            // плавное появление: выезд слева + проявление
            float t = Math.min(1f, (System.currentTimeMillis() - start) / 420f);
            float e = 1f - (1f - t) * (1f - t) * (1f - t);
            int ox = (int) (-(1f - e) * 60f);
            float a = e;

            // клики (ловим нажатие левой кнопки сами)
            boolean down = mc.mouseHandler.isLeftPressed();
            boolean click = down && !wasDown;
            wasDown = down;
            if (!down) dragging = false;

            // раскладка
            int leftSpace = sw / 2 - 102;
            int cw = Math.max(112, Math.min(170, leftSpace - 16));
            int x0 = 8 + ox;
            int y0 = 6;
            int cx = x0 + cw / 2;
            int r = Math.max(18, Math.min(30, Math.min(cw / 2 - 24, (sh - 210) / 2)));
            int cy = y0 + 88 + r;
            int textY = cy + r + 8;
            int barY = cy + r + 40;
            int barX = x0 + 8;
            int barW = cw - 16;
            int loopY = barY + 16;
            int addY = loopY + 20;
            int y1 = addY + 16 + 8;
            int playY = cy - r - 14;
            int arrowDx = r + 17;

            boolean playing = MusicPlayer.isPlaying();

            // карточка
            g.fill(x0 - 1, y0 - 1, x0 + cw + 1, y1 + 1, argb(0xFFFFFF, 0.10f * a));
            g.fill(x0, y0, x0 + cw, y1, argb(0x0E0E14, 0.82f * a));
            g.fill(x0, y0, x0 + cw, y0 + 2, argb(ACCENT, 0.9f * a));

            // инструкция
            text(g, font, "Инструкция", x0 + 8, y0 + 7, 0xFFFFFF, a);
            String[] help = {
                    "Положи .wav или .mp3",
                    "в папку musicloader",
                    "или перетащи в окно.",
                    "Иконка: .png с тем же",
                    "именем (не обязательно)"
            };
            for (int i = 0; i < help.length; i++) {
                text(g, font, fit(font, help[i], cw - 16), x0 + 8, y0 + 18 + i * 9, 0x9A9AA8, a);
            }

            // кружок
            circle(g, cx, cy, r, argb(playing ? ACCENT : 0x6A6A78, a));
            circle(g, cx, cy, r - 2, argb(0x1A1A22, a));

            var song = MusicPlayer.current();
            if (song != null && song.icon() != null && e > 0.6f) {
                Identifier icon = iconFor(song.icon());
                if (icon != null) {
                    int size = (int) (r * 1.4);
                    g.blit(RenderPipelines.GUI_TEXTURED, icon, cx - size / 2, cy - size / 2,
                            0f, 0f, size, size, size, size);
                }
            }

            // кнопка пауза / пуск (по центру сверху)
            boolean hoverPlay = overCircle(mouseX, mouseY, cx, playY, 11);
            circle(g, cx, playY, 11, argb(hoverPlay ? 0xA6EF2A : ACCENT, a));
            int ic = argb(0x101010, a);
            if (playing) {
                g.fill(cx - 4, playY - 5, cx - 1, playY + 5, ic);
                g.fill(cx + 1, playY - 5, cx + 4, playY + 5, ic);
            } else {
                for (int dx = 0; dx < 8; dx++) {
                    int hh = 5 - (dx * 5) / 8;
                    g.fill(cx - 3 + dx, playY - hh, cx - 2 + dx, playY + hh, ic);
                }
            }
            if (click && hoverPlay) MusicPlayer.togglePause();

            // стрелки влево / вправо
            boolean hoverL = overCircle(mouseX, mouseY, cx - arrowDx, cy, 10);
            boolean hoverR = overCircle(mouseX, mouseY, cx + arrowDx, cy, 10);
            circle(g, cx - arrowDx, cy, 10, argb(hoverL ? 0x4A4A58 : 0x2A2A34, a));
            circle(g, cx + arrowDx, cy, 10, argb(hoverR ? 0x4A4A58 : 0x2A2A34, a));
            centered(g, font, "<", cx - arrowDx, cy - 4, 0xFFFFFF, a);
            centered(g, font, ">", cx + arrowDx, cy - 4, 0xFFFFFF, a);
            if (click && hoverL) MusicPlayer.previous();
            if (click && hoverR) MusicPlayer.next();

            // название, автор, таймкод
            String title = song == null ? "Нет песни" : song.title();
            String author = song == null ? "Выбери стрелками" : song.author();
            if (MusicPlayer.isLoading()) author = "Загрузка...";
            centered(g, font, fit(font, title, cw - 12), cx, textY, 0xFFFFFF, a);
            centered(g, font, fit(font, author, cw - 12), cx, textY + 10, 0x9A9AA8, a);
            centered(g, font, time(MusicPlayer.positionSeconds()) + " / " + time(MusicPlayer.lengthSeconds()),
                    cx, textY + 20, playing ? ACCENT : 0x9A9AA8, a);

            // полоса прогресса (клик или перетаскивание = перемотка)
            boolean hoverBar = over(mouseX, mouseY, barX, barY - 4, barW, 14);
            if (click && hoverBar) dragging = true;
            if (dragging && down) MusicPlayer.seek((mouseX - barX) / (double) barW);
            g.fill(barX, barY, barX + barW, barY + 6, argb(0x34343E, a));
            int fill = (int) (barW * MusicPlayer.progress());
            if (fill > 0) g.fill(barX, barY, barX + fill, barY + 6, argb(ACCENT, a));
            circle(g, barX + fill, barY + 3, 4, argb(0xFFFFFF, a));

            // кнопка "Повтор"
            boolean loop = MusicPlayer.isLoop();
            boolean hoverLoop = over(mouseX, mouseY, x0 + 8, loopY, cw - 16, 16);
            box(g, x0 + 8, loopY, cw - 16, 16,
                    argb(loop ? 0x2C4A00 : (hoverLoop ? 0x34343E : 0x24242C), a),
                    argb(loop ? ACCENT : 0x4A4A58, a));
            centered(g, font, loop ? "Повтор: вкл" : "Повтор: выкл", cx, loopY + 4,
                    loop ? ACCENT : 0xCCCCCC, a);
            if (click && hoverLoop) MusicPlayer.toggleLoop();

            // кнопка "Добавить текст"
            boolean hoverAdd = over(mouseX, mouseY, x0 + 8, addY, cw - 16, 16);
            box(g, x0 + 8, addY, cw - 16, 16,
                    argb(hoverAdd ? 0x34343E : 0x24242C, a), argb(0x4A4A58, a));
            centered(g, font, "Добавить текст", cx, addY + 4, 0xCCCCCC, a);
            if (click && hoverAdd) ScreenUtil.open(new TextInputScreen(screen));

            // подпись автора мода в углу
            String credit = "creator:caat_coooll";
            text(g, font, credit, sw - font.width(credit) - 6, 6, 0xB8B8C8, a);

            // свой текст внизу по центру
            String custom = MusicLibrary.cachedText();
            if (!custom.isEmpty()) {
                text(g, font, custom, (sw - font.width(custom)) / 2, sh - 14, 0xFFFFFF, a);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {}
    }
}
