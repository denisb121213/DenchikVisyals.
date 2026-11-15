package com.denchik.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.GenericContainerScreenHandler;
import org.lwjgl.glfw.GLFW;

/** Кнопки «Сложить», «Сложить всё» и «Забрать всё» над окном сундука. */
public final class ChestButtons {

    private static final String[] LABELS = {"Сложить", "Сложить всё", "Забрать всё"};
    private static boolean prevDown = false;

    private ChestButtons() {}

    public static void render(DrawContext ctx, Screen screen, int mouseX, int mouseY) {
        if (!Config.chestButtons) return;
        MinecraftClient c = MinecraftClient.getInstance();
        if (!(screen instanceof GenericContainerScreen) || c.player == null
                || !(c.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) {
            return;
        }

        TextRenderer tr = c.textRenderer;
        long window = c.getWindow().getHandle();
        boolean down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean clicked = down && !prevDown;
        prevDown = down;
        // если в руке предмет, клики по кнопкам не обрабатываем (чтобы он не выпал)
        boolean canClick = handler.getCursorStack().isEmpty();

        int bgW = 176;
        int bgH = 114 + handler.getRows() * 18;
        int gx = (screen.width - bgW) / 2;
        int gy = (screen.height - bgH) / 2;
        int by = Math.max(2, gy - 15);
        int accent = 0xFF000000 | Config.accentRgb();

        int x = gx + bgW;
        for (int i = LABELS.length - 1; i >= 0; i--) {
            int bw = tr.getWidth(LABELS[i]) + 10;
            x -= bw;
            boolean hov = mouseX >= x && mouseX < x + bw && mouseY >= by && mouseY < by + 13;
            ctx.fill(x, by, x + bw, by + 13, hov ? ((accent & 0x00FFFFFF) | 0xD0000000) : 0xC0101018);
            ctx.fill(x, by + 12, x + bw, by + 13, accent);
            ctx.drawTextWithShadow(tr, LABELS[i], x + 5, by + 3, 0xFFFFFFFF);
            if (clicked && hov && canClick) {
                Effects.chestAction(c, i);
            }
            x -= 3;
        }
    }
}
