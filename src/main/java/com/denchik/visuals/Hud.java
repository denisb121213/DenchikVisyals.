package com.denchik.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Информационный HUD: водяной знак, FPS, координаты, клавиши, броня, информация о цели. */
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
        if (Config.hudPing) lines.add("Пинг: " + ping(c, p) + " мс");
        if (Config.hudCoords) {
            lines.add(String.format("XYZ: %.0f %.0f %.0f", p.getX(), p.getY(), p.getZ()));
        }
        if (Config.hudDirection) lines.add("Курс: " + DIRS[MathHelper.floor(p.getYaw() / 45.0f + 0.5f) & 7]);
        if (Config.hudTime) lines.add("Время: " + LocalTime.now().format(TIME));
        if (Config.hudCps) lines.add("CPS: " + Effects.cpsLeft() + " | " + Effects.cpsRight());
        if (!lines.isEmpty()) {
            infoPanel(ctx, tr, 4, y, lines, accent);
        }

        if (Config.hudKeys) keys(ctx, c, tr, w - 68, h - 80, accent);
        if (Config.hudArmor) armor(ctx, tr, p, w - 22, h / 2 - 36);
        if (Config.targetHud) target(ctx, c, tr, w / 2 + 18, h / 2 + 8, accent);
    }

    private static int ping(MinecraftClient c, ClientPlayerEntity p) {
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

    private static void target(DrawContext ctx, MinecraftClient c, TextRenderer tr, int x, int y, int accent) {
        if (!(c.targetedEntity instanceof LivingEntity t) || !t.isAlive()) {
            hpShown = -1f;
            return;
        }
        float hp = t.getHealth();
        float max = Math.max(1f, t.getMaxHealth());
        hpShown = hpShown < 0f ? hp : hpShown + (hp - hpShown) * 0.15f;

        String name = t.getName().getString();
        if (name.length() > 18) name = name.substring(0, 18);
        int bw = 118;
        int bh = 32;
        ctx.fill(x, y, x + bw, y + bh, 0xA0101018);
        ctx.fill(x, y, x + 2, y + bh, accent);
        ctx.drawTextWithShadow(tr, name, x + 7, y + 4, 0xFFFFFFFF);
        String hpText = String.format("%.1f / %.0f", hp, max);
        ctx.drawTextWithShadow(tr, hpText, x + 7, y + 14, 0xFFB8B8D0);

        float frac = MathHelper.clamp(hpShown / max, 0f, 1f);
        int bx1 = x + 7;
        int bx2 = x + bw - 7;
        int by = y + 25;
        ctx.fill(bx1, by, bx2, by + 3, 0xFF2A2A3E);
        int fx = bx1 + (int) ((bx2 - bx1) * frac);
        ctx.fill(bx1, by, fx, by + 3, 0xFF000000 | MathHelper.hsvToRgb(frac * 0.33f, 0.8f, 1f));
    }
}
