package com.denchik.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ThreadLocalRandom;

/** Горячая клавиша, шляпы и частицы. Вызывается каждый тик игры. */
public final class Effects {

    private static boolean prevKey = false;
    private static int ticks = 0;

    private Effects() {}

    public static void tick(MinecraftClient client) {
        handleKey(client);

        ClientPlayerEntity p = client.player;
        ClientWorld w = client.world;
        if (p == null || w == null || client.isPaused()) return;

        ticks++;

        boolean firstPerson = client.options.getPerspective().isFirstPerson();
        if (Config.hat != Config.Hat.NONE && !firstPerson) {
            spawnHat(client, p);
        }
        if (Config.fx != Config.Fx.NONE) {
            spawnFx(client, p);
        }
    }

    // ---------- Right Shift ----------

    private static void handleKey(MinecraftClient client) {
        long handle = client.getWindow().getHandle();
        boolean down = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (down && !prevKey) {
            if (client.currentScreen == null && client.player != null) {
                client.setScreen(new VisualsScreen());
            } else if (client.currentScreen instanceof VisualsScreen) {
                client.setScreen(null);
            }
        }
        prevKey = down;
    }

    // ---------- Цвета ----------

    private static int rainbow(double offset) {
        double t = (System.currentTimeMillis() % 4000L) / 4000.0;
        float hue = (float) ((t + offset) % 1.0);
        return MathHelper.hsvToRgb(hue, 0.85f, 1.0f);
    }

    private static ParticleEffect hatColor(double offset, float scale) {
        int rgb = Config.hatRainbow ? rainbow(offset) : MathHelper.hsvToRgb(Config.hatHue, 1.0f, 1.0f);
        return new DustParticleEffect(rgb, scale);
    }

    private static void add(MinecraftClient c, ParticleEffect e, double x, double y, double z, double vx, double vy, double vz) {
        c.particleManager.addParticle(e, x, y, z, vx, vy, vz);
    }

    // ---------- Шляпы (из частиц) ----------

    private static void ring(MinecraftClient c, double cx, double cy, double cz, double radius, int points,
                             double vx, double vz) {
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            ParticleEffect e = hatColor(i / (double) points, 0.55f);
            add(c, e, cx + Math.cos(a) * radius, cy, cz + Math.sin(a) * radius, vx, 0, vz);
        }
    }

    private static void spawnHat(MinecraftClient c, ClientPlayerEntity p) {
        if (ticks % 2 != 0) return;
        double x = p.getX();
        double z = p.getZ();
        double y = p.getY() + p.getHeight() + 0.1;
        Vec3d v = p.getVelocity();
        double vx = v.x;
        double vz = v.z;

        switch (Config.hat) {
            case HALO -> ring(c, x, y + 0.3, z, 0.34, 16, vx, vz);
            case WIZARD -> {
                ring(c, x, y, z, 0.62, 18, vx, vz);
                ring(c, x, y + 0.02, z, 0.5, 14, vx, vz);
                for (int k = 1; k <= 5; k++) {
                    double h = k * 0.2;
                    double r = 0.4 * (1.0 - h / 1.15);
                    ring(c, x, y + h, z, r, Math.max(5, 12 - k * 2), vx, vz);
                }
            }
            case CROWN -> {
                ring(c, x, y, z, 0.3, 14, vx, vz);
                ring(c, x, y + 0.12, z, 0.3, 14, vx, vz);
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI * 2 / 6;
                    for (int k = 0; k < 3; k++) {
                        add(c, hatColor(i / 6.0, 0.55f),
                                x + Math.cos(a) * 0.3, y + 0.24 + k * 0.1, z + Math.sin(a) * 0.3, vx, 0, vz);
                    }
                }
            }
            case TOP_HAT -> {
                ring(c, x, y, z, 0.5, 18, vx, vz);
                ring(c, x, y + 0.12, z, 0.27, 10, vx, vz);
                ring(c, x, y + 0.3, z, 0.27, 10, vx, vz);
                ring(c, x, y + 0.48, z, 0.27, 10, vx, vz);
                ring(c, x, y + 0.62, z, 0.27, 10, vx, vz);
                ring(c, x, y + 0.62, z, 0.13, 6, vx, vz);
            }
            default -> {
            }
        }
    }

    // ---------- Частицы ----------

    private static double spread(ThreadLocalRandom r, double range) {
        return (r.nextDouble() - 0.5) * 2.0 * range;
    }

    private static void spawnFx(MinecraftClient c, ClientPlayerEntity p) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double x = p.getX();
        double y = p.getY();
        double z = p.getZ();
        double h = p.getHeight();
        int n = Config.fxAmount;

        switch (Config.fx) {
            case HEARTS -> {
                if (ticks % 4 == 0) {
                    for (int i = 0; i < Math.max(1, n / 3); i++) {
                        add(c, ParticleTypes.HEART, x + spread(r, 0.6), y + h * 0.5 + r.nextDouble() * h,
                                z + spread(r, 0.6), 0, 0.05, 0);
                    }
                }
            }
            case FLAMES -> {
                for (int i = 0; i < Math.max(1, n / 2); i++) {
                    add(c, ParticleTypes.FLAME, x + spread(r, 0.4), y + r.nextDouble() * 0.3,
                            z + spread(r, 0.4), 0, 0.03, 0);
                }
            }
            case SNOW -> {
                for (int i = 0; i < n; i++) {
                    add(c, ParticleTypes.SNOWFLAKE, x + spread(r, 4), y + 1 + r.nextDouble() * 3,
                            z + spread(r, 4), 0, -0.05, 0);
                }
            }
            case STARS -> {
                for (int i = 0; i < Math.max(1, n / 2); i++) {
                    add(c, ParticleTypes.END_ROD, x + spread(r, 1.2), y + r.nextDouble() * (h + 0.5),
                            z + spread(r, 1.2), spread(r, 0.02), 0.02, spread(r, 0.02));
                }
            }
            case NOTES -> {
                if (ticks % 3 == 0) {
                    for (int i = 0; i < Math.max(1, n / 3); i++) {
                        add(c, ParticleTypes.NOTE, x + spread(r, 0.7), y + h + 0.3 + r.nextDouble() * 0.5,
                                z + spread(r, 0.7), r.nextDouble(), 0, 0);
                    }
                }
            }
            case RAINBOW_TRAIL -> {
                for (int i = 0; i < Math.max(1, n / 2); i++) {
                    add(c, new DustParticleEffect(rainbow(i * 0.1), 1.0f), x + spread(r, 0.3), y + 0.1,
                            z + spread(r, 0.3), 0, 0.01, 0);
                }
            }
            case SPIRAL -> {
                double a = ticks * 0.35;
                double yy = y + ((ticks * 0.05) % 1.0) * (h + 0.4);
                for (int k = 0; k < 3; k++) {
                    double aa = a + k * Math.PI * 2 / 3;
                    add(c, new DustParticleEffect(rainbow(k / 3.0), 0.8f),
                            x + Math.cos(aa) * 0.7, yy, z + Math.sin(aa) * 0.7, 0, 0, 0);
                }
            }
            default -> {
            }
        }
    }
}
