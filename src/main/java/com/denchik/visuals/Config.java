package com.denchik.visuals;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Все настройки мода. Хранятся в config/denchikvisuals.properties */
public final class Config {

    public enum SkyMode {
        OFF("Выкл"), COLOR("Свой цвет"), RAINBOW("Радуга");
        public final String label;
        SkyMode(String label) { this.label = label; }
    }

    public enum Hat {
        NONE("Нет"), HALO("Нимб"), WIZARD("Колпак мага"), CROWN("Корона"), TOP_HAT("Цилиндр");
        public final String label;
        Hat(String label) { this.label = label; }
    }

    public enum Fx {
        NONE("Нет"), HEARTS("Сердечки"), FLAMES("Огонь"), SNOW("Снег"),
        STARS("Звёзды"), NOTES("Ноты"), RAINBOW_TRAIL("Радужный след"), SPIRAL("Спираль");
        public final String label;
        Fx(String label) { this.label = label; }
    }

    public static SkyMode skyMode = SkyMode.OFF;
    public static int skyR = 140, skyG = 70, skyB = 255;
    public static boolean fogFollowsSky = true;
    public static float rainbowSpeed = 0.4f;

    public static Hat hat = Hat.NONE;
    public static float hatHue = 0.13f;
    public static boolean hatRainbow = false;

    public static Fx fx = Fx.NONE;
    public static int fxAmount = 4;

    /** Цвет неба в формате ARGB. */
    public static int skyColor() {
        int rgb;
        if (skyMode == SkyMode.RAINBOW) {
            long period = (long) (20000 - 18000 * rainbowSpeed);
            if (period < 1000) period = 1000;
            float hue = (System.currentTimeMillis() % period) / (float) period;
            rgb = MathHelper.hsvToRgb(hue, 0.65f, 1.0f);
        } else {
            rgb = (skyR << 16) | (skyG << 8) | skyB;
        }
        return 0xFF000000 | rgb;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("denchikvisuals.properties");
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("skyMode", skyMode.name());
        p.setProperty("skyR", String.valueOf(skyR));
        p.setProperty("skyG", String.valueOf(skyG));
        p.setProperty("skyB", String.valueOf(skyB));
        p.setProperty("fogFollowsSky", String.valueOf(fogFollowsSky));
        p.setProperty("rainbowSpeed", String.valueOf(rainbowSpeed));
        p.setProperty("hat", hat.name());
        p.setProperty("hatHue", String.valueOf(hatHue));
        p.setProperty("hatRainbow", String.valueOf(hatRainbow));
        p.setProperty("fx", fx.name());
        p.setProperty("fxAmount", String.valueOf(fxAmount));
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
        try {
            skyMode = SkyMode.valueOf(p.getProperty("skyMode", skyMode.name()));
            skyR = clampInt(p.getProperty("skyR"), skyR);
            skyG = clampInt(p.getProperty("skyG"), skyG);
            skyB = clampInt(p.getProperty("skyB"), skyB);
            fogFollowsSky = Boolean.parseBoolean(p.getProperty("fogFollowsSky", String.valueOf(fogFollowsSky)));
            rainbowSpeed = Float.parseFloat(p.getProperty("rainbowSpeed", String.valueOf(rainbowSpeed)));
            hat = Hat.valueOf(p.getProperty("hat", hat.name()));
            hatHue = Float.parseFloat(p.getProperty("hatHue", String.valueOf(hatHue)));
            hatRainbow = Boolean.parseBoolean(p.getProperty("hatRainbow", String.valueOf(hatRainbow)));
            fx = Fx.valueOf(p.getProperty("fx", fx.name()));
            fxAmount = Math.max(1, Math.min(10, clampInt(p.getProperty("fxAmount"), fxAmount)));
        } catch (Exception ignored) {
        }
    }

    private static int clampInt(String s, int def) {
        if (s == null) return def;
        try {
            return Math.max(0, Math.min(255, Integer.parseInt(s.trim())));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
