package com.denchik.visuals;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Все настройки мода. Хранятся в config/denchikvisuals.properties */
public final class Config {

    public enum SkyMode {
        OFF("Выкл"), COLOR("Свой цвет"), RAINBOW("Радуга"), PULSE("Пульс");
        public final String label;
        SkyMode(String label) { this.label = label; }
    }

    public enum Hat {
        NONE("Нет"), HALO("Нимб"), WIZARD("Колпак"), CROWN("Корона"), TOP_HAT("Цилиндр"),
        CAT_EARS("Ушки"), HORNS("Рога");
        public final String label;
        Hat(String label) { this.label = label; }
    }

    public enum Fx {
        NONE("Нет"), HEARTS("Сердца"), FLAMES("Огонь"), SNOW("Снег"), STARS("Звёзды"),
        NOTES("Ноты"), RAINBOW_TRAIL("Радуга"), SPIRAL("Спираль"), MAGIC("Магия"),
        SOUL("Души"), ELECTRIC("Искры"), TOTEM("Тотем");
        public final String label;
        Fx(String label) { this.label = label; }
    }

    public enum Crosshair {
        OFF("Обычный"), CROSS("Крест"), DOT("Точка"), CIRCLE("Круг");
        public final String label;
        Crosshair(String label) { this.label = label; }
    }

    /** Готовые цвета неба (R, G, B). */
    public static final int[][] PRESETS = {
            {140, 70, 255}, {255, 120, 70}, {40, 140, 255}, {80, 255, 120},
            {255, 150, 200}, {25, 15, 45}, {170, 230, 255}, {160, 20, 30}
    };

    // ---------- Мир ----------
    public static SkyMode skyMode = SkyMode.OFF;
    public static int skyR = 140;
    public static int skyG = 70;
    public static int skyB = 255;
    public static float rainbowSpeed = 0.4f;
    public static boolean fogFollowsSky = true;
    public static boolean cloudsFollowSky = true;
    public static boolean sunsetFollowsSky = true;
    public static boolean starsAlways = false;

    // ---------- Шляпы ----------
    public static Hat hat = Hat.NONE;
    public static float hatHue = 0.13f;
    public static boolean hatRainbow = false;
    public static float hatSize = 1.0f;

    // ---------- Частицы ----------
    public static Fx fx = Fx.NONE;
    public static int fxAmount = 4;

    // ---------- HUD ----------
    public static boolean hudWatermark = true;
    public static boolean hudFps = true;
    public static boolean hudPing = false;
    public static boolean hudCoords = true;
    public static boolean hudDirection = false;
    public static boolean hudTime = false;
    public static boolean hudCps = true;
    public static boolean hudSpeed = false;

    // ---------- Бой (только визуал и информация) ----------
    public static boolean targetHud = true;
    public static boolean hitEffect = false;
    public static boolean hudKeys = false;
    public static boolean hudArmor = true;
    public static boolean hudEffects = false;
    public static boolean hudItems = false;
    public static boolean hudCombo = false;
    public static Crosshair crosshair = Crosshair.OFF;
    public static int crosshairSize = 5;
    public static float crosshairHue = 1.0f;
    public static boolean crosshairReact = true;

    // ---------- Игрок ----------
    public static boolean nightVision = false;
    public static boolean quickStack = true;
    public static boolean playerArrows = false;
    public static int arrowRange = 100;

    // ---------- Камера ----------
    public static boolean zoomEnabled = true;
    public static int zoomFov = 30;

    // ---------- Тема ----------
    public static float accentHue = 0.75f;
    public static boolean accentRainbow = false;
    public static boolean animations = true;
    public static float panelAlpha = 0.92f;

    private Config() {}

    /** Цвет неба в формате ARGB. */
    public static int skyColor() {
        int r = skyR;
        int g = skyG;
        int b = skyB;
        if (skyMode == SkyMode.RAINBOW) {
            long period = (long) (20000 - 18000 * rainbowSpeed);
            if (period < 1000) period = 1000;
            float hue = (System.currentTimeMillis() % period) / (float) period;
            int rgb = MathHelper.hsvToRgb(hue, 0.65f, 1.0f);
            r = (rgb >> 16) & 255;
            g = (rgb >> 8) & 255;
            b = rgb & 255;
        } else if (skyMode == SkyMode.PULSE) {
            long period = (long) (8000 - 6500 * rainbowSpeed);
            if (period < 1000) period = 1000;
            double t = (System.currentTimeMillis() % period) / (double) period;
            float f = 0.55f + 0.45f * (float) (0.5 + 0.5 * Math.sin(t * Math.PI * 2));
            r = (int) (r * f);
            g = (int) (g * f);
            b = (int) (b * f);
        }
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Цвет акцента панели и HUD (RGB без прозрачности). */
    public static int accentRgb() {
        float hue = accentRainbow ? (System.currentTimeMillis() % 6000L) / 6000f : accentHue;
        return MathHelper.hsvToRgb(hue, 0.6f, 1.0f);
    }

    // ---------- Сохранение ----------

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("denchikvisuals.properties");
    }

    private static boolean isSetting(Field f) {
        int m = f.getModifiers();
        return Modifier.isStatic(m) && Modifier.isPublic(m) && !Modifier.isFinal(m);
    }

    public static void save() {
        Properties p = new Properties();
        for (Field f : Config.class.getDeclaredFields()) {
            if (!isSetting(f)) continue;
            try {
                p.setProperty(f.getName(), String.valueOf(f.get(null)));
            } catch (IllegalAccessException ignored) {
            }
        }
        try (Writer w = Files.newBufferedWriter(file())) {
            p.store(w, "DenchikVisuals");
        } catch (IOException ignored) {
        }
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(f)) {
            p.load(r);
        } catch (IOException e) {
            return;
        }
        for (Field field : Config.class.getDeclaredFields()) {
            if (!isSetting(field)) continue;
            String s = p.getProperty(field.getName());
            if (s == null) continue;
            s = s.trim();
            try {
                Class<?> t = field.getType();
                if (t == int.class) {
                    field.setInt(null, Integer.parseInt(s));
                } else if (t == float.class) {
                    field.setFloat(null, Float.parseFloat(s));
                } else if (t == boolean.class) {
                    field.setBoolean(null, Boolean.parseBoolean(s));
                } else if (t.isEnum()) {
                    for (Object c : t.getEnumConstants()) {
                        if (((Enum<?>) c).name().equals(s)) {
                            field.set(null, c);
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        sanitize();
    }

    private static int clampI(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float clampF(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    public static void sanitize() {
        skyR = clampI(skyR, 0, 255);
        skyG = clampI(skyG, 0, 255);
        skyB = clampI(skyB, 0, 255);
        rainbowSpeed = clampF(rainbowSpeed, 0f, 1f);
        hatHue = clampF(hatHue, 0f, 1f);
        hatSize = clampF(hatSize, 0.5f, 2f);
        fxAmount = clampI(fxAmount, 1, 10);
        zoomFov = clampI(zoomFov, 30, 60);
        accentHue = clampF(accentHue, 0f, 1f);
        panelAlpha = clampF(panelAlpha, 0.5f, 1f);
        crosshairSize = clampI(crosshairSize, 2, 12);
        crosshairHue = clampF(crosshairHue, 0f, 1f);
        arrowRange = clampI(arrowRange, 20, 300);
    }
}
