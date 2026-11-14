package com.denchik.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.concurrent.ThreadLocalRandom;

/** Горячие клавиши, шляпы, частицы, зум, подсчёт CPS. Вызывается каждый тик игры. */
public final class Effects {

    private static boolean prevShift = false;
    private static boolean prevLeft = false;
    private static boolean prevRight = false;
    private static boolean zooming = false;
    private static int originalFov = 70;
    private static int ticks = 0;

    public static boolean leftDown = false;
    public static boolean rightDown = false;
    private static final ArrayDeque<Long> leftClicks = new ArrayDeque<>();
    private static final ArrayDeque<Long> rightClicks = new ArrayDeque<>();

    private Effects() {}

    public static int cpsLeft() {
        return count(leftClicks);
    }

    public static int cpsRight() {
        return count(rightClicks);
    }

    private static int count(ArrayDeque<Long> q) {
        long now = System.currentTimeMillis();
        while (!q.isEmpty() && now - q.peekFirst() > 1000L) {
            q.pollFirst();
        }
        return q.size();
    }

    public static void tick(MinecraftClient client) {
        long handle = client.getWindow().getHandle();
        handleKey(client, handle);
        pollMouse(client, handle);
        handleZoom(client, handle);

        ClientPlayerEntity p = client.player;
        if (p == null || client.world == null || client.isPaused()) return;

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

    private static void handleKey(MinecraftClient client, long handle) {
        boolean down = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (down && !prevShift) {
            if (client.currentScreen == null && client.player != null) {
                client.setScreen(new VisualsScreen());
            } else if (client.currentScreen instanceof VisualsScreen) {
                client.setScreen(null);
            }
        }
        prevShift = down;
    }

    // ---------- Мышь: CPS и эффект удара ----------

    private static void pollMouse(MinecraftClient client, long handle) {
        boolean l = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean inGame = client.currentScreen == null && client.player != null;
        leftDown = l && inGame;
        rightDown = r && inGame;
        long now = System.currentTimeMillis();
        if (inGame) {
            if (l && !prevLeft) {
                leftClicks.addLast(now);
                onAttackClick(client);
            }
            if (r && !prevRight) {
                rightClicks.addLast(now);
            }
        }
        prevLeft = l;
        prevRight = r;
    }

    private static void onAttackClick(MinecraftClient client) {
        if (!Config.hitEffect) return;
        Entity t = client.targetedEntity;
        if (t == null || t == client.player) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double cx = t.getX();
        double cy = t.getY() + t.getHeight() * 0.5;
        double cz = t.getZ();
        for (int i = 0; i < 10; i++) {
            ParticleEffect e = (i % 2 == 0) ? ParticleTypes.CRIT : ParticleTypes.ENCHANTED_HIT;
            add(client, e, cx + spread(r, 0.3), cy + spread(r, 0.3), cz + spread(r, 0.3),
                    spread(r, 0.3), 0.15 + r.nextDouble() * 0.2, spread(r, 0.3));
        }
    }

    // ---------- Зум (удерживай C) ----------

    private static void handleZoom(MinecraftClient client, long handle) {
        boolean want = Config.zoomEnabled
                && client.currentScreen == null
                && client.player != null
                && GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_C) == GLFW.GLFW_PRESS;
        if (want && !zooming) {
            originalFov = client.options.getFov().getValue();
            client.options.getFov().setValue(Config.zoomFov);
            zooming = true;
        } else if (!want && zooming) {
            client.options.getFov().setValue(originalFov);
            zooming = false;
        }
    }

    // ---------- Цвета и частицы ----------

    private static int rainbow(double offset) {
        double t = (System.currentTimeMillis() % 4000L) / 4000.0;
        float hue = (float) ((t + offset) % 1.0);
        return MathHelper.hsvToRgb(hue, 0.85f, 1.0f);
    }

    private static ParticleEffect hatColor(double offset, float scale) {
        int rgb = Config.hatRainbow ? rainbow(offset) : MathHelper.hsvToRgb(Config.hatHue, 1.0f, 1.0f);
        return new DustParticleEffect(rgb, scale);
    }

    private static void add(MinecraftClient c, ParticleEffect e, double x, double y, double z,
                            double vx, double vy, double vz) {
        c.particleManager.addParticle(e, x, y, z, vx, vy, vz);
    }

    private static double spread(ThreadLocalRandom r, double range) {
        return (r.nextDouble() - 0.5) * 2.0 * range;
    }

    // ---------- Шляпы (из частиц) ----------

    private static void ring(MinecraftClient c, double cx, double cy, double cz, double radius, int points,
                             double vx, double vz) {
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            add(c, hatColor(i / (double) points, 0.55f),
                    cx + Math.cos(a) * radius, cy, cz + Math.sin(a) * radius, vx, 0, vz);
        }
    }

    private static void spawnHat(MinecraftClient c, ClientPlayerEntity p) {
        if (ticks % 2 != 0) return;
        double s = Config.hatSize;
        double x = p.getX();
        double z = p.getZ();
        double y = p.getY() + p.getHeight() + 0.1;
        Vec3d v = p.getVelocity();
        double vx = v.x;
        double vz = v.z;

        switch (Config.hat) {
            case HALO -> ring(c, x, y + 0.3 * s, z, 0.34 * s, 16, vx, vz);
            case WIZARD -> {
                ring(c, x, y, z, 0.62 * s, 18, vx, vz);
                ring(c, x, y + 0.02 * s, z, 0.5 * s, 14, vx, vz);
                for (int k = 1; k <= 5; k++) {
                    double h = k * 0.2;
                    double r = 0.4 * (1.0 - h / 1.15);
                    ring(c, x, y + h * s, z, r * s, Math.max(5, 12 - k * 2), vx, vz);
                }
            }
            case CROWN -> {
                ring(c, x, y, z, 0.3 * s, 14, vx, vz);
                ring(c, x, y + 0.12 * s, z, 0.3 * s, 14, vx, vz);
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI * 2 / 6;
                    for (int k = 0; k < 3; k++) {
                        add(c, hatColor(i / 6.0, 0.55f),
                                x + Math.cos(a) * 0.3 * s, y + (0.24 + k * 0.1) * s, z + Math.sin(a) * 0.3 * s,
                                vx, 0, vz);
                    }
                }
            }
            case TOP_HAT -> {
                ring(c, x, y, z, 0.5 * s, 18, vx, vz);
                ring(c, x, y + 0.12 * s, z, 0.27 * s, 10, vx, vz);
                ring(c, x, y + 0.3 * s, z, 0.27 * s, 10, vx, vz);
                ring(c, x, y + 0.48 * s, z, 0.27 * s, 10, vx, vz);
                ring(c, x, y + 0.62 * s, z, 0.27 * s, 10, vx, vz);
                ring(c, x, y + 0.62 * s, z, 0.13 * s, 6, vx, vz);
            }
            case CAT_EARS -> {
                double yaw = Math.toRadians(p.getYaw());
                double rx = -Math.cos(yaw);
                double rz = -Math.sin(yaw);
                for (int side = -1; side <= 1; side += 2) {
                    double bx = x + rx * 0.22 * s * side;
                    double bz = z + rz * 0.22 * s * side;
                    for (int k = 0; k < 4; k++) {
                        double h = k * 0.07 * s;
                        double half = 0.1 * s * (1.0 - k / 4.0);
                        int pts = Math.max(2, 4 - k);
                        for (int j = 0; j < pts; j++) {
                            double t = (j / (double) (pts - 1)) * 2 - 1;
                            add(c, hatColor(j / 6.0, 0.5f),
                                    bx + rx * half * t, y + h, bz + rz * half * t, vx, 0, vz);
                        }
                    }
                }
            }
            case HORNS -> {
                double yaw = Math.toRadians(p.getYaw());
                double rx = -Math.cos(yaw);
                double rz = -Math.sin(yaw);
                for (int side = -1; side <= 1; side += 2) {
                    for (int k = 0; k < 7; k++) {
                        double off = 0.18 + 0.03 * k - 0.004 * k * k;
                        double h = 0.06 * k + 0.01 * k * k;
                        add(c, hatColor(k / 7.0, 0.5f),
                                x + rx * off * s * side, y + h * s, z + rz * off * s * side, vx, 0, vz);
                    }
                }
            }
            default -> {
            }
        }
    }

    // ---------- Частицы вокруг игрока ----------

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
            case MAGIC -> {
                for (int i = 0; i < Math.max(1, n / 2); i++) {
                    add(c, ParticleTypes.ENCHANT, x + spread(r, 0.5), y + h + 0.5 + r.nextDouble() * 0.4,
                            z + spread(r, 0.5), spread(r, 0.5), -0.5 - r.nextDouble() * 0.5, spread(r, 0.5));
                }
            }
            case SOUL -> {
                for (int i = 0; i < Math.max(1, n / 2); i++) {
                    add(c, ParticleTypes.SOUL_FIRE_FLAME, x + spread(r, 0.4), y + r.nextDouble() * 0.4,
                            z + spread(r, 0.4), 0, 0.03, 0);
                }
            }
            case ELECTRIC -> {
                for (int i = 0; i < Math.max(1, n / 2); i++) {
                    add(c, ParticleTypes.ELECTRIC_SPARK, x + spread(r, 0.5), y + r.nextDouble() * h,
                            z + spread(r, 0.5), spread(r, 0.3), spread(r, 0.3), spread(r, 0.3));
                }
            }
            case TOTEM -> {
                for (int i = 0; i < Math.max(1, n / 2); i++) {
                    add(c, ParticleTypes.TOTEM_OF_UNDYING, x + spread(r, 0.5), y + r.nextDouble() * h,
                            z + spread(r, 0.5), spread(r, 0.1), 0.1 + r.nextDouble() * 0.1, spread(r, 0.1));
                }
            }
            default -> {
            }
        }
    }
}
