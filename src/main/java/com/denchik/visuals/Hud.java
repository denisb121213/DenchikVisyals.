package com.denchik.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Информационный HUD: название, FPS, координаты, клавиши, броня, цель, эффекты, прицел, стрелки к игрокам. */
public final class Hud {

    private static int fpsCount = 0;
    private static int fpsShown = 0;
    private static long fpsWindow = 0L;
    private static float hpShown = -1f;

    private static final String[] DIRS = {"Ю", "ЮЗ", "З", "СЗ", "С", "СВ", "В", "ЮВ"};
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static ItemStack[] iconStacks = null;

    private Hud() {}

    public static void render(DrawContext ctx) {
        MinecraftClient c = MinecraftClient.getInstance();
        ClientPlayerEntity p = c.player;
        if (p == null || c.world == null || c.options.hudHidden) return;
        TextRenderer tr = c.textRenderer;

        long now = System.currentTimeMillis();
        fpsCount++;
        if (now - fpsWindow >= 1000L) {
            fpsShown = fpsCount;
            fpsCount = 0;
            fpsWindow = now;
        }

        int accent = 0xFF000000 | Config.accentRgb();
        int w = c.getWindow().getScaledWidth();
        int h = c.getWindow().getScaledHeight();

        int y = 4;
        if (Config.hudWatermark) {
            y = watermark(ctx, tr, 4, 4, accent) + 3;
        }

        List<String> lines = new ArrayList<>();
        if (Config.hudFps) lines.add("FPS: " + fpsShown);
        if (Config.hudPing) lines.add("Пинг: " + ping(p) + " мс");
        if (Config.hudCoords) {
            lines.add(String.format("XYZ: %.0f %.0f %.0f", p.getX(), p.getY(), p.getZ()));
        }
        if (Config.hudDirection) lines.add("Курс: " + DIRS[MathHelper.floor(p.getYaw() / 45.0f + 0.5f) & 7]);
        if (Config.hudTime) lines.add("Время: " + LocalTime.now().format(TIME));
        if (Config.hudSpeed) lines.add(String.format("Скорость: %.1f б/с", Effects.speed()));
        if (Config.hudCps) lines.add("CPS: " + Effects.cpsLeft() + " | " + Effects.cpsRight());
        if (Config.hudCombo) lines.add("Комбо: " + Effects.combo());
        if (!lines.isEmpty()) {
            infoPanel(ctx, tr, 4, y, lines, accent);
        }

        if (Config.hudEffects) effects(ctx, tr, p, 4, h / 2 - 70);
        if (Config.hudItems) items(ctx, tr, p, 4, h / 2 + 20);
        if (Config.hudKeys) keys(ctx, c, tr, w - 68, h - 80, accent);
        if (Config.hudArmor) armor(ctx, tr, p, w - 22, h / 2 - 36);
        if (Config.targetHud) target(ctx, c, tr, p, w / 2 + 18, h / 2 + 8, accent);
        if (Config.crosshair != Config.Crosshair.OFF) crosshair(ctx, c, w, h);
        if (Config.playerArrows) arrows(ctx, c, tr, p, w, h, accent);
    }

    private static int ping(ClientPlayerEntity p) {
        if (p.networkHandler == null) return 0;
        PlayerListEntry e = p.networkHandler.getPlayerListEntry(p.getUuid());
        return e == null ? 0 : e.getLatency();
    }

    private static void rainbowText(DrawContext ctx, TextRenderer tr, String s, int x, int y) {
        long t = System.currentTimeMillis();
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            float hue = (((t % 3000L) / 3000f) + i * 0.05f) % 1f;
            ctx.drawTextWithShadow(tr, ch, cx, y, 0xFF000000 | MathHelper.hsvToRgb(hue, 0.6f, 1f));
            cx += tr.getWidth(ch);
        }
    }

    private static int watermark(DrawContext ctx, TextRenderer tr, int x, int y, int accent) {
        String name = "DenchikVisuals";
        int bw = tr.getWidth(name) + 12;
        ctx.fill(x, y, x + bw, y + 14, 0xA0101018);
        ctx.fill(x, y, x + 2, y + 14, accent);
        rainbowText(ctx, tr, name, x + 6, y + 3);
        return y + 14;
    }

    private static void infoPanel(DrawContext ctx, TextRenderer tr, int x, int y, List<String> lines, int accent) {
        int max = 0;
        for (String s : lines) max = Math.max(max, tr.getWidth(s));
        int bw = max + 12;
        int bh = lines.size() * 11 + 4;
        ctx.fill(x, y, x + bw, y + bh, 0xA0101018);
        ctx.fill(x, y, x + 2, y + bh, accent);
        for (int i = 0; i < lines.size(); i++) {
            ctx.drawTextWithShadow(tr, lines.get(i), x + 6, y + 3 + i * 11, 0xFFFFFFFF);
        }
    }

    // ---------- Клавиши ----------

    private static void keyBox(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h,
                               String label, boolean pressed, int accent) {
        ctx.fill(x, y, x + w, y + h, pressed ? ((accent & 0x00FFFFFF) | 0xC8000000) : 0x90101018);
        int tw = tr.getWidth(label);
        ctx.drawTextWithShadow(tr, label, x + (w - tw) / 2, y + (h - 8) / 2, 0xFFFFFFFF);
    }

    private static void keys(DrawContext ctx, MinecraftClient c, TextRenderer tr, int x, int y, int accent) {
        keyBox(ctx, tr, x + 22, y, 20, 20, "W", c.options.forwardKey.isPressed(), accent);
        keyBox(ctx, tr, x, y + 22, 20, 20, "A", c.options.leftKey.isPressed(), accent);
        keyBox(ctx, tr, x + 22, y + 22, 20, 20, "S", c.options.backKey.isPressed(), accent);
        keyBox(ctx, tr, x + 44, y + 22, 20, 20, "D", c.options.rightKey.isPressed(), accent);
        keyBox(ctx, tr, x, y + 44, 64, 12, "—", c.options.jumpKey.isPressed(), accent);
        keyBox(ctx, tr, x, y + 58, 31, 16, "Л " + Effects.cpsLeft(), Effects.leftDown, accent);
        keyBox(ctx, tr, x + 33, y + 58, 31, 16, "П " + Effects.cpsRight(), Effects.rightDown, accent);
    }

    // ---------- Броня ----------

    private static void armor(DrawContext ctx, TextRenderer tr, ClientPlayerEntity p, int x, int y0) {
        for (int i = 0; i < ARMOR.length; i++) {
            ItemStack s = p.getEquippedStack(ARMOR[i]);
            if (s.isEmpty()) continue;
            int y = y0 + i * 18;
            ctx.drawItem(s, x, y);
            if (s.isDamageable()) {
                int max = s.getMaxDamage();
                int left = max - s.getDamage();
                int pct = max <= 0 ? 100 : (left * 100 / max);
                String t = pct + "%";
                int col = 0xFF000000 | MathHelper.hsvToRgb(pct / 100f * 0.33f, 0.8f, 1f);
                ctx.drawTextWithShadow(tr, t, x - 3 - tr.getWidth(t), y + 4, col);
            }
        }
    }

    // ---------- Эффекты зелий с таймерами ----------

    private static void effects(DrawContext ctx, TextRenderer tr, ClientPlayerEntity p, int x, int y0) {
        int y = y0;
        int shown = 0;
        for (StatusEffectInstance e : p.getStatusEffects()) {
            if (shown >= 8) break;
            String name = e.getEffectType().value().getName().getString();
            if (e.getAmplifier() > 0) name += " " + (e.getAmplifier() + 1);
            String time;
            if (e.isInfinite()) {
                time = "--";
            } else {
                int sec = Math.max(0, e.getDuration() / 20);
                time = (sec / 60) + ":" + (sec % 60 < 10 ? "0" : "") + (sec % 60);
            }
            String line = name + "  " + time;
            int bw = tr.getWidth(line) + 10;
            ctx.fill(x, y, x + bw, y + 11, 0x90101018);
            int col = (!e.isInfinite() && e.getDuration() < 200) ? 0xFFFF6060 : 0xFFFFFFFF;
            ctx.drawTextWithShadow(tr, line, x + 5, y + 2, col);
            y += 12;
            shown++;
        }
    }

    // ---------- Счётчик предметов ----------

    private static int count(ClientPlayerEntity p, Item... wanted) {
        int n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty()) continue;
            for (Item it : wanted) {
                if (s.isOf(it)) n += s.getCount();
            }
        }
        return n;
    }

    private static void items(DrawContext ctx, TextRenderer tr, ClientPlayerEntity p, int x, int y0) {
        if (iconStacks == null) {
            iconStacks = new ItemStack[]{
                    new ItemStack(Items.TOTEM_OF_UNDYING),
                    new ItemStack(Items.GOLDEN_APPLE),
                    new ItemStack(Items.ENDER_PEARL),
                    new ItemStack(Items.ARROW)
            };
        }
        int[] counts = {
                count(p, Items.TOTEM_OF_UNDYING),
                count(p, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE),
                count(p, Items.ENDER_PEARL),
                count(p, Items.ARROW)
        };
        for (int i = 0; i < iconStacks.length; i++) {
            int y = y0 + i * 18;
            ctx.fill(x, y, x + 40, y + 17, 0x90101018);
            ctx.drawItem(iconStacks[i], x + 1, y);
            String n = String.valueOf(counts[i]);
            ctx.drawTextWithShadow(tr, n, x + 20, y + 5, counts[i] == 0 ? 0xFF808090 : 0xFFFFFFFF);
        }
    }

    // ---------- Информация о цели ----------

    private static void target(DrawContext ctx, MinecraftClient c, TextRenderer tr, ClientPlayerEntity p,
                               int x, int y, int accent) {
        if (!(c.targetedEntity instanceof LivingEntity t) || !t.isAlive()) {
            hpShown = -1f;
            return;
        }
        float hp = t.getHealth();
        float max = Math.max(1f, t.getMaxHealth());
        hpShown = hpShown < 0f ? hp : hpShown + (hp - hpShown) * 0.15f;

        String name = t.getName().getString();
        if (name.length() > 18) name = name.substring(0, 18);
        int bw = 136;
        int bh = 32;
        ctx.fill(x, y, x + bw, y + bh, 0xA0101018);
        ctx.fill(x, y, x + 2, y + bh, accent);
        ctx.drawTextWithShadow(tr, name, x + 7, y + 4, 0xFFFFFFFF);
        String hpText = String.format("%.1f / %.0f   %.1f м", hp, max, p.distanceTo(t));
        ctx.drawTextWithShadow(tr, hpText, x + 7, y + 14, 0xFFB8B8D0);

        float frac = MathHelper.clamp(hpShown / max, 0f, 1f);
        int bx1 = x + 7;
        int bx2 = x + bw - 7;
        int by = y + 25;
        ctx.fill(bx1, by, bx2, by + 3, 0xFF2A2A3E);
        int fx = bx1 + (int) ((bx2 - bx1) * frac);
        ctx.fill(bx1, by, fx, by + 3, 0xFF000000 | MathHelper.hsvToRgb(frac * 0.33f, 0.8f, 1f));
    }

    // ---------- Свой прицел ----------

    private static void crosshair(DrawContext ctx, MinecraftClient c, int w, int h) {
        int cx = w / 2;
        int cy = h / 2;
        int s = Config.crosshairSize;
        int col;
        if (Config.crosshairReact && c.targetedEntity != null) {
            col = 0xFFFF3030;
        } else {
            float sat = Config.crosshairHue > 0.97f ? 0f : 0.9f;
            col = 0xFF000000 | MathHelper.hsvToRgb(Config.crosshairHue, sat, 1f);
        }
        switch (Config.crosshair) {
            case CROSS -> {
                int gap = 2;
                ctx.fill(cx - gap - s, cy - 1, cx - gap, cy + 1, col);
                ctx.fill(cx + gap, cy - 1, cx + gap + s, cy + 1, col);
                ctx.fill(cx - 1, cy - gap - s, cx + 1, cy - gap, col);
                ctx.fill(cx - 1, cy + gap, cx + 1, cy + gap + s, col);
            }
            case DOT -> ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, col);
            case CIRCLE -> {
                for (int i = 0; i < 32; i++) {
                    double a = i * Math.PI * 2 / 32;
                    int px = cx + (int) Math.round(Math.cos(a) * s);
                    int py = cy + (int) Math.round(Math.sin(a) * s);
                    ctx.fill(px, py, px + 1, py + 1, col);
                }
            }
            default -> {
            }
        }
    }

    // ---------- Стрелки к игрокам ----------

    private static void dot(DrawContext ctx, double x, double y, int col) {
        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);
        ctx.fill(ix, iy, ix + 2, iy + 2, col);
    }

    private static void arrows(DrawContext ctx, MinecraftClient c, TextRenderer tr, ClientPlayerEntity p,
                               int w, int h, int accent) {
        // В одиночной игре и LAN видны все игроки, на серверах - только своя команда.
        boolean solo = c.isInSingleplayer();
        int radius = Math.max(30, Math.min(70, Math.min(w, h) / 3));
        int cx = w / 2;
        int cy = h / 2;

        for (var o : c.world.getPlayers()) {
            if (o == p) continue;
            if (!solo && !p.isTeammate(o)) continue;
            float dist = p.distanceTo(o);
            if (dist > Config.arrowRange) continue;

            double dx = o.getX() - p.getX();
            double dz = o.getZ() - p.getZ();
            float rel = MathHelper.wrapDegrees((float) (Math.toDegrees(Math.atan2(-dx, dz)) - p.getYaw()));
            double rad = Math.toRadians(rel);
            double ux = Math.sin(rad);
            double uy = -Math.cos(rad);
            double tipX = cx + ux * radius;
            double tipY = cy + uy * radius;

            for (int i = 0; i <= 9; i++) {
                dot(ctx, tipX - ux * i, tipY - uy * i, accent);
            }
            for (int side = -1; side <= 1; side += 2) {
                double ang = Math.toRadians(150.0 * side);
                double hx = ux * Math.cos(ang) - uy * Math.sin(ang);
                double hy = ux * Math.sin(ang) + uy * Math.cos(ang);
                for (int i = 1; i <= 5; i++) {
                    dot(ctx, tipX + hx * i, tipY + hy * i, accent);
                }
            }

            String label = o.getName().getString() + " " + Math.round(dist) + "м";
            int tw = tr.getWidth(label);
            int lx = (int) Math.round(tipX + ux * 12) - tw / 2;
            int ly = (int) Math.round(tipY + uy * 12) - 4;
            ctx.drawTextWithShadow(tr, label, lx, ly, 0xFFFFFFFF);
        }
    }
}
