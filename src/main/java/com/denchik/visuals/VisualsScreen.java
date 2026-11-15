package com.denchik.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/** Меню DenchikVisuals (Right Shift). Нарисовано вручную, с анимациями. */
public class VisualsScreen extends Screen {

    private static final String[] TABS = {"Мир", "Шляпы", "Частицы", "HUD", "Бой", "Игрок", "Камера", "FPS", "Тема"};
    private static final int ROW = 20;

    private int tab = 0;
    private float tabPos = 0f;
    private float open = 0f;
    private float scroll = 0f;
    private float scrollTarget = 0f;
    private float contentHeight = 0f;
    private long lastNs = 0L;
    private boolean prevDown = false;
    private boolean down = false;
    private boolean clicked = false;
    private boolean pendingSave = false;
    private String dragging = null;
    private final Map<String, Float> anims = new HashMap<>();

    // значения на время одного кадра
    private DrawContext ctx;
    private TextRenderer tr;
    private int mx;
    private int my;
    private float dt;
    private float fade;
    private int accent;
    private int cx1;
    private int cy1;
    private int cx2;
    private int cy2;
    private float rowY;
    private boolean inContent;

    public VisualsScreen() {
        super(Text.literal("DenchikVisuals"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // Колесо мыши (без @Override: если сигнатура другая, просто не будет прокрутки)
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollTarget -= (float) verticalAmount * 26f;
        if (scrollTarget < 0f) scrollTarget = 0f;
        return true;
    }

    public void removed() {
        Config.sanitize();
        Config.save();
    }

    // ---------- Вспомогательные функции ----------

    private static int argb(float a, int rgb) {
        int ai = Math.max(0, Math.min(255, (int) (a * 255f)));
        return (ai << 24) | (rgb & 0xFFFFFF);
    }

    private static int mix(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static boolean animOn() {
        return Config.animations && !Config.ecoMode;
    }

    private float anim(String id, float target, float speed) {
        if (!animOn()) return target;
        Float v = anims.get(id);
        if (v == null) v = target;
        v += (target - v) * Math.min(1f, dt * speed);
        anims.put(id, v);
        return v;
    }

    private void fill(int x1, int y1, int x2, int y2, int rgb, float a) {
        if (a <= 0.003f || x2 <= x1 || y2 <= y1) return;
        ctx.fill(x1, y1, x2, y2, argb(a, rgb));
    }

    /** Прямоугольник со скруглёнными углами (без наложения, чтобы не было полос). */
    private void rrect(int x1, int y1, int x2, int y2, int rgb, float a) {
        if (x2 - x1 < 6 || y2 - y1 < 6) {
            fill(x1, y1, x2, y2, rgb, a);
            return;
        }
        fill(x1 + 2, y1, x2 - 2, y1 + 1, rgb, a);
        fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, rgb, a);
        fill(x1, y1 + 2, x2, y2 - 2, rgb, a);
        fill(x1 + 1, y2 - 2, x2 - 1, y2 - 1, rgb, a);
        fill(x1 + 2, y2 - 1, x2 - 2, y2, rgb, a);
    }

    private void hgrad(int x1, int y1, int x2, int y2, int c1, int c2, float a) {
        int w = x2 - x1;
        for (int i = 0; i < w; i += 2) {
            float t = i / (float) Math.max(1, w - 1);
            fill(x1 + i, y1, Math.min(x2, x1 + i + 2), y2, mix(c1, c2, t), a);
        }
    }

    private void text(String s, int x, int y, int rgb, float a) {
        if (a < 0.08f) return;
        ctx.drawTextWithShadow(tr, s, x, y, argb(a, rgb));
    }

    private boolean hover(int x1, int y1, int x2, int y2) {
        return inContent && mx >= x1 && mx < x2 && my >= y1 && my < y2;
    }

    // ---------- Элементы меню ----------

    private void section(String s) {
        rowY += 2;
        int y = (int) rowY;
        text(s, cx1 + 4, y, accent, fade);
        fill(cx1 + 4 + tr.getWidth(s) + 5, y + 4, cx2 - 4, y + 5, accent, 0.25f * fade);
        rowY += 13;
    }

    private void note(String s) {
        text(s, cx1 + 4, (int) rowY, 0x8A8AA0, fade);
        rowY += 11;
    }

    private boolean toggle(String id, String label, boolean value) {
        int y = (int) rowY;
        int x1 = cx1 + 2;
        int x2 = cx2 - 2;
        boolean hov = hover(x1, y, x2, y + ROW - 2);
        float h = anim("h:" + id, hov ? 1f : 0f, 14f);
        rrect(x1, y, x2, y + ROW - 2, mix(0x1E1E2C, 0x2A2A3E, h), fade);
        text(label, x1 + 7, y + 5, 0xEDEDF7, fade);

        float t = anim("t:" + id, value ? 1f : 0f, 16f);
        int sw = 24;
        int sh = 10;
        int sx2 = x2 - 7;
        int sx1 = sx2 - sw;
        int sy = y + (ROW - 2 - sh) / 2;
        rrect(sx1, sy, sx2, sy + sh, mix(0x3A3A4E, accent, t), fade);
        int kx = sx1 + 1 + (int) (t * (sw - sh));
        fill(kx, sy + 1, kx + sh - 2, sy + sh - 1, 0xFFFFFF, fade);

        rowY += ROW;
        if (clicked && hov) {
            pendingSave = true;
            return !value;
        }
        return value;
    }

    private float slider(String id, String label, float value, float min, float max, int digits) {
        int y = (int) rowY;
        int x1 = cx1 + 2;
        int x2 = cx2 - 2;
        int bx1 = x1 + 7;
        int bx2 = x2 - 7;
        int by = y + 14;
        boolean hov = hover(x1, y, x2, y + ROW + 4);
        if (clicked && hov) dragging = id;
        if (id.equals(dragging) && down) {
            float ff = MathHelper.clamp((mx - bx1) / (float) (bx2 - bx1), 0f, 1f);
            float nv = min + ff * (max - min);
            if (nv != value) {
                value = nv;
                pendingSave = true;
            }
        }
        float f = MathHelper.clamp((value - min) / (max - min), 0f, 1f);
        float sf = anim("s:" + id, f, 30f);
        float h = anim("h:" + id, hov ? 1f : 0f, 14f);

        rrect(x1, y, x2, y + ROW + 2, mix(0x1E1E2C, 0x2A2A3E, h), fade);
        text(label, x1 + 7, y + 3, 0xEDEDF7, fade);
        String val = String.format("%." + digits + "f", value);
        text(val, x2 - 7 - tr.getWidth(val), y + 3, 0xB8B8D0, fade);

        rrect(bx1, by, bx2, by + 4, 0x34344A, fade);
        int fx = bx1 + (int) ((bx2 - bx1) * sf);
        fill(bx1, by, Math.max(bx1 + 1, fx), by + 4, accent, fade);
        fill(fx - 2, by - 2, fx + 2, by + 6, 0xFFFFFF, fade);

        rowY += ROW + 6;
        return value;
    }

    private int chips(String id, String[] labels, int selected) {
        int x = cx1 + 2;
        int y = (int) rowY;
        int right = cx2 - 2;
        int result = selected;
        for (int i = 0; i < labels.length; i++) {
            int w = tr.getWidth(labels[i]) + 14;
            if (x + w > right) {
                x = cx1 + 2;
                y += 20;
            }
            boolean hov = hover(x, y, x + w, y + 17);
            boolean sel = i == selected;
            float s = anim("c:" + id + i, sel ? 1f : 0f, 14f);
            float h = anim("ch:" + id + i, hov ? 1f : 0f, 14f);
            int bg = mix(mix(0x1E1E2C, 0x2C2C42, h), accent, s * 0.55f);
            rrect(x, y, x + w, y + 17, bg, fade);
            text(labels[i], x + 7, y + 5, sel ? 0xFFFFFF : 0xC4C4DA, fade);
            if (clicked && hov) {
                result = i;
                pendingSave = true;
            }
            x += w + 4;
        }
        rowY = y + 22;
        return result;
    }

    private boolean button(String id, String label, String sub) {
        int y = (int) rowY;
        int x1 = cx1 + 2;
        int x2 = cx2 - 2;
        boolean hov = hover(x1, y, x2, y + ROW - 2);
        float h = anim("b:" + id, hov ? 1f : 0f, 14f);
        rrect(x1, y, x2, y + ROW - 2, mix(mix(0x1E1E2C, 0x2A2A3E, h), accent, h * 0.35f), fade);
        fill(x1, y + 3, x1 + 2, y + ROW - 5, accent, fade);
        text(label, x1 + 8, y + 5, 0xFFFFFF, fade);
        text(sub, x2 - 7 - tr.getWidth(sub), y + 5, 0xA8A8C0, fade);
        rowY += ROW;
        return clicked && hov;
    }

    private void colorBar(int rgb) {
        int y = (int) rowY;
        rrect(cx1 + 2, y, cx2 - 2, y + 8, rgb, fade);
        rowY += 12;
    }

    private void presets() {
        int n = Config.PRESETS.length;
        int gap = 4;
        int total = cx2 - cx1 - 4;
        int w = (total - gap * (n - 1)) / n;
        int x = cx1 + 2;
        int y = (int) rowY + 2;
        for (int i = 0; i < n; i++) {
            int[] c = Config.PRESETS[i];
            int rgb = (c[0] << 16) | (c[1] << 8) | c[2];
            boolean hov = hover(x, y - 2, x + w, y + 16);
            float h = anim("p:" + i, hov ? 1f : 0f, 14f);
            rrect(x, y - (int) (h * 2f), x + w, y + 16, rgb, fade);
            if (clicked && hov) {
                Config.skyR = c[0];
                Config.skyG = c[1];
                Config.skyB = c[2];
                if (Config.skyMode == Config.SkyMode.OFF || Config.skyMode == Config.SkyMode.RAINBOW) {
                    Config.skyMode = Config.SkyMode.COLOR;
                }
                pendingSave = true;
            }
            x += w + gap;
        }
        rowY += 24;
    }

    // ---------- Вкладки ----------

    private void buildWorld() {
        section("НЕБО");
        Config.SkyMode[] modes = Config.SkyMode.values();
        String[] labels = new String[modes.length];
        for (int i = 0; i < modes.length; i++) labels[i] = modes[i].label;
        int mode = chips("skymode", labels, Config.skyMode.ordinal());
        Config.skyMode = modes[mode];

        section("ЦВЕТ");
        presets();
        int r = Math.round(slider("r", "Красный", Config.skyR, 0, 255, 0));
        int g = Math.round(slider("g", "Зелёный", Config.skyG, 0, 255, 0));
        int b = Math.round(slider("b", "Синий", Config.skyB, 0, 255, 0));
        if (r != Config.skyR || g != Config.skyG || b != Config.skyB) {
            Config.skyR = r;
            Config.skyG = g;
            Config.skyB = b;
            if (Config.skyMode == Config.SkyMode.OFF || Config.skyMode == Config.SkyMode.RAINBOW) {
                Config.skyMode = Config.SkyMode.COLOR;
            }
        }
        colorBar(Config.skyColor() & 0xFFFFFF);
        Config.rainbowSpeed = slider("rs", "Скорость радуги и пульса", Config.rainbowSpeed, 0f, 1f, 2);

        section("ДОПОЛНИТЕЛЬНО");
        Config.fogFollowsSky = toggle("fog", "Туман в цвет неба", Config.fogFollowsSky);
        Config.cloudsFollowSky = toggle("clouds", "Облака в цвет неба", Config.cloudsFollowSky);
        Config.sunsetFollowsSky = toggle("sunset", "Закат в цвет неба", Config.sunsetFollowsSky);
        Config.starsAlways = toggle("stars", "Звёзды видны днём", Config.starsAlways);
    }

    private void buildHats() {
        section("ШЛЯПА");
        Config.Hat[] hats = Config.Hat.values();
        String[] labels = new String[hats.length];
        for (int i = 0; i < hats.length; i++) labels[i] = hats[i].label;
        int idx = chips("hat", labels, Config.hat.ordinal());
        Config.hat = hats[idx];

        section("ВИД");
        Config.hatHue = slider("hh", "Цвет шляпы", Config.hatHue, 0f, 1f, 2);
        colorBar(MathHelper.hsvToRgb(Config.hatHue, 1f, 1f));
        Config.hatRainbow = toggle("hr", "Радужная шляпа", Config.hatRainbow);
        Config.hatSize = slider("hs", "Размер", Config.hatSize, 0.5f, 2f, 2);
        note("Шляпы видны от третьего лица (F5).");
        note("Их видишь только ты.");
    }

    private void buildFx() {
        section("ЧАСТИЦЫ");
        Config.Fx[] fxs = Config.Fx.values();
        String[] labels = new String[fxs.length];
        for (int i = 0; i < fxs.length; i++) labels[i] = fxs[i].label;
        int idx = chips("fx", labels, Config.fx.ordinal());
        Config.fx = fxs[idx];

        section("НАСТРОЙКА");
        Config.fxAmount = Math.round(slider("fa", "Количество", Config.fxAmount, 1, 10, 0));
        note("Частицы видишь только ты.");
    }

    private void buildHud() {
        section("ИНФОРМАЦИЯ");
        Config.hudWatermark = toggle("hw", "Название DenchikVisuals", Config.hudWatermark);
        Config.hudFps = toggle("hf", "FPS", Config.hudFps);
        Config.hudPing = toggle("hp", "Пинг", Config.hudPing);
        Config.hudCoords = toggle("hc", "Координаты", Config.hudCoords);
        Config.hudDirection = toggle("hd", "Направление", Config.hudDirection);
        Config.hudTime = toggle("ht", "Время", Config.hudTime);
        Config.hudCps = toggle("hcps", "CPS (клики в секунду)", Config.hudCps);
        Config.hudSpeed = toggle("hsp", "Скорость (блоков в секунду)", Config.hudSpeed);
    }

    private void buildCombat() {
        section("БОЙ (ИНФОРМАЦИЯ)");
        Config.targetHud = toggle("th", "Здоровье и дистанция до цели", Config.targetHud);
        Config.hitEffect = toggle("he", "Частицы при ударе", Config.hitEffect);
        Config.hudKeys = toggle("hk", "Клавиши (WASD, мышь)", Config.hudKeys);
        Config.hudArmor = toggle("ha", "Прочность брони", Config.hudArmor);
        Config.hudEffects = toggle("hef", "Эффекты и таймеры", Config.hudEffects);
        Config.hudItems = toggle("hit", "Тотемы, яблоки, жемчуг, стрелы", Config.hudItems);
        Config.hudCombo = toggle("hcb", "Комбо (удары подряд)", Config.hudCombo);

        section("ПРИЦЕЛ");
        Config.Crosshair[] cs = Config.Crosshair.values();
        String[] labels = new String[cs.length];
        for (int i = 0; i < cs.length; i++) labels[i] = cs[i].label;
        int idx = chips("cross", labels, Config.crosshair.ordinal());
        Config.crosshair = cs[idx];
        Config.crosshairSize = Math.round(slider("cs", "Размер прицела", Config.crosshairSize, 2, 12, 0));
        Config.crosshairHue = slider("ch", "Цвет (в самом конце - белый)", Config.crosshairHue, 0f, 1f, 2);
        Config.crosshairReact = toggle("cr", "Краснеет, когда смотришь на цель", Config.crosshairReact);
        rowY += 4;
        note("Только информация и внешний вид.");
        note("Ничего, что играет за тебя.");
    }

    private void buildPlayer() {
        section("ВИДИМОСТЬ");
        Config.nightVision = toggle("nv", "Ночное зрение", Config.nightVision);
        note("Видишь в темноте только ты.");

        section("ИНВЕНТАРЬ");
        Config.quickStack = toggle("qs", "Быстро сложить в сундук (V)", Config.quickStack);
        Config.chestButtons = toggle("cb", "Кнопки над окном сундука", Config.chestButtons);
        Config.quickDelay = Math.round(slider("qd", "Пауза между кликами (тики)", Config.quickDelay, 1, 6, 0));
        note("Клавиша V или кнопки над сундуком:");
        note("Сложить - то, что там уже есть.");
        note("Сложить всё - всё, кроме хотбара.");
        note("Забрать всё - достать всё из сундука.");

        section("СТРЕЛКИ К ИГРОКАМ");
        Config.playerArrows = toggle("pa2", "Показывать стрелки", Config.playerArrows);
        Config.arrowRange = Math.round(slider("ar2", "Дальность (блоков)", Config.arrowRange, 20, 300, 0));
        note("Одиночная игра и LAN: все игроки.");
        note("На серверах: только твоя команда.");
    }

    private String perfMsg = "";

    private void buildPerf() {
        section("ЭКО-РЕЖИМ МОДА");
        Config.ecoMode = toggle("eco", "Эко-режим (меньше частиц, без анимаций)", Config.ecoMode);
        note("Шляпы и частицы создаются реже,");
        note("меню работает без анимаций.");

        section("НАСТРОЙКИ ИГРЫ ОДНОЙ КНОПКОЙ");
        MinecraftClient mc = MinecraftClient.getInstance();
        if (button("pf0", "Слабый ПК", "дальность 6, без облаков")) {
            Perf.apply(mc, 0);
            perfMsg = "Применено: Слабый ПК";
        }
        if (button("pf1", "Средний ПК", "дальность 10")) {
            Perf.apply(mc, 1);
            perfMsg = "Применено: Средний ПК";
        }
        if (button("pf2", "Мощный ПК (красиво)", "всё включено")) {
            Perf.apply(mc, 2);
            perfMsg = "Применено: Мощный ПК";
        }
        if (!perfMsg.isEmpty()) {
            note(perfMsg);
        }
        rowY += 2;
        note("Ещё сильнее поднимает FPS мод Sodium");
        note("(скачай на modrinth.com).");
    }

    private void buildCamera() {
        section("ЗУМ");
        Config.zoomEnabled = toggle("ze", "Зум (держи клавишу C)", Config.zoomEnabled);
        Config.zoomFov = Math.round(slider("zf", "Угол обзора при зуме", Config.zoomFov, 30, 60, 0));
        note("Меньше значение = сильнее приближение.");
    }

    private void buildTheme() {
        section("ЦВЕТ ПАНЕЛИ");
        Config.accentHue = slider("ah", "Цвет акцента", Config.accentHue, 0f, 1f, 2);
        Config.accentRainbow = toggle("ar", "Радужный акцент", Config.accentRainbow);
        section("ВНЕШНИЙ ВИД");
        Config.panelAlpha = slider("pa", "Прозрачность панели", Config.panelAlpha, 0.5f, 1f, 2);
        Config.animations = toggle("an", "Анимации", Config.animations);
    }

    // ---------- Отрисовка ----------

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);

        this.ctx = c;
        this.tr = this.textRenderer;
        this.mx = mouseX;
        this.my = mouseY;

        long now = System.nanoTime();
        dt = lastNs == 0L ? 0.016f : Math.min(0.05f, (now - lastNs) / 1.0e9f);
        lastNs = now;

        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        down = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        clicked = down && !prevDown;
        boolean released = !down && prevDown;
        prevDown = down;
        if (released) dragging = null;

        if (animOn()) {
            open = Math.min(1f, open + dt * 4.5f);
        } else {
            open = 1f;
        }
        float e = 1f - (1f - open) * (1f - open) * (1f - open);
        fade = e;
        float panelA = e * Config.panelAlpha;

        accent = Config.accentRgb();
        int accent2 = MathHelper.hsvToRgb(((Config.accentRainbow
                ? (System.currentTimeMillis() % 6000L) / 6000f : Config.accentHue) + 0.12f) % 1f, 0.7f, 0.95f);

        int pw = Math.min(this.width - 16, 430);
        int ph = Math.min(this.height - 16, 262);
        int px = (this.width - pw) / 2;
        int py = (this.height - ph) / 2 + (int) ((1f - e) * 16f);

        // тень и фон
        fill(px - 3, py - 3, px + pw + 3, py + ph + 3, 0x000000, 0.35f * e);
        rrect(px, py, px + pw, py + ph, 0x0E0E16, panelA);

        // шапка
        hgrad(px + 1, py + 1, px + pw - 1, py + 28, mix(accent, 0x101018, 0.45f), mix(accent2, 0x101018, 0.65f), 0.9f * e);
        drawTitle(px + 10, py + 6);
        text("визуалы для Minecraft 1.21.11", px + 10, py + 17, 0xC9C9E0, fade);

        // боковая панель с вкладками
        int tx1 = px + 6;
        int tx2 = px + 88;
        int ty = py + 36;
        tabPos = animOn() ? tabPos + (tab - tabPos) * Math.min(1f, dt * 14f) : tab;
        int hy = ty + (int) (tabPos * 21f);
        rrect(tx1, hy, tx2, hy + 19, accent, 0.28f * e);
        fill(tx1, hy + 3, tx1 + 2, hy + 16, accent, e);
        for (int i = 0; i < TABS.length; i++) {
            int y = ty + i * 21;
            boolean hov = mx >= tx1 && mx < tx2 && my >= y && my < y + 19;
            float h = anim("tab" + i, hov ? 1f : 0f, 14f);
            int col = i == tab ? 0xFFFFFF : mix(0x9A9AB2, 0xFFFFFF, h);
            text(TABS[i], tx1 + 10 + (int) (h * 2f), y + 6, col, fade);
            if (clicked && hov && i != tab) {
                tab = i;
                scroll = 0f;
                scrollTarget = 0f;
            }
        }

        // область содержимого
        cx1 = px + 94;
        cy1 = py + 34;
        cx2 = px + pw - 8;
        cy2 = py + ph - 20;
        rrect(cx1 - 3, cy1 - 3, cx2 + 3, cy2 + 3, 0x14141E, panelA);
        inContent = mx >= cx1 && mx <= cx2 && my >= cy1 && my <= cy2;

        scroll = animOn() ? scroll + (scrollTarget - scroll) * Math.min(1f, dt * 14f) : scrollTarget;
        float top = cy1 - scroll + 2f;
        rowY = top;

        ctx.enableScissor(cx1, cy1, cx2, cy2);
        switch (tab) {
            case 0 -> buildWorld();
            case 1 -> buildHats();
            case 2 -> buildFx();
            case 3 -> buildHud();
            case 4 -> buildCombat();
            case 5 -> buildPlayer();
            case 6 -> buildCamera();
            case 7 -> buildPerf();
            default -> buildTheme();
        }
        ctx.disableScissor();

        contentHeight = rowY - top;
        float maxScroll = Math.max(0f, contentHeight - (cy2 - cy1) + 4f);
        if (scrollTarget > maxScroll) scrollTarget = maxScroll;
        if (scrollTarget < 0f) scrollTarget = 0f;
        if (maxScroll > 0f) {
            int trackH = cy2 - cy1;
            int thumbH = Math.max(14, (int) (trackH * trackH / (contentHeight + 4f)));
            int thumbY = cy1 + (int) ((trackH - thumbH) * (scroll / maxScroll));
            fill(cx2 + 1, thumbY, cx2 + 3, thumbY + thumbH, accent, 0.8f * fade);
        }

        // подвал
        text("RShift / Esc - закрыть     C - зум", px + 10, py + ph - 13, 0x7C7C94, fade);
        String ver = "v1.3.0";
        text(ver, px + pw - 10 - tr.getWidth(ver), py + ph - 13, 0x7C7C94, fade);

        // сохранение, когда мышь отпущена
        if (pendingSave && !down) {
            Config.sanitize();
            Config.save();
            pendingSave = false;
        }
    }

    private void drawTitle(int x, int y) {
        String s = "DenchikVisuals";
        long t = System.currentTimeMillis();
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            float hue = (((t % 3000L) / 3000f) + i * 0.05f) % 1f;
            text(ch, cx, y, MathHelper.hsvToRgb(hue, 0.5f, 1f), fade);
            cx += tr.getWidth(ch);
        }
    }
}
