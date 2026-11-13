package com.denchik.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.DoubleConsumer;

/** Меню по Right Shift. */
public class VisualsScreen extends Screen {

    private static final int COL_W = 150;
    private static final int GAP = 10;

    private ButtonWidget skyButton;
    private int left, right, top;

    public VisualsScreen() {
        super(Text.literal("DenchikVisuals"));
    }

    private static <E extends Enum<E>> E next(E e) {
        E[] all = e.getDeclaringClass().getEnumConstants();
        return all[(e.ordinal() + 1) % all.length];
    }

    private static Text yesNo(String name, boolean v) {
        return Text.literal(name + ": " + (v ? "Да" : "Нет"));
    }

    @Override
    protected void init() {
        left = this.width / 2 - COL_W - GAP / 2;
        right = this.width / 2 + GAP / 2;
        top = Math.max(50, this.height / 2 - 85);

        int y = top;

        // ---------- Левая колонка: небо ----------
        skyButton = ButtonWidget.builder(Text.empty(), b -> {
            Config.skyMode = next(Config.skyMode);
            b.setMessage(Text.literal("Небо: " + Config.skyMode.label));
            Config.save();
        }).dimensions(left, y, COL_W, 20).build();
        skyButton.setMessage(Text.literal("Небо: " + Config.skyMode.label));
        addDrawableChild(skyButton);

        addDrawableChild(new ValueSlider(left, y + 24, COL_W, 20, "Красный", 0, 255, Config.skyR, true, v -> {
            Config.skyR = (int) v;
            autoEnableSky();
        }));
        addDrawableChild(new ValueSlider(left, y + 48, COL_W, 20, "Зелёный", 0, 255, Config.skyG, true, v -> {
            Config.skyG = (int) v;
            autoEnableSky();
        }));
        addDrawableChild(new ValueSlider(left, y + 72, COL_W, 20, "Синий", 0, 255, Config.skyB, true, v -> {
            Config.skyB = (int) v;
            autoEnableSky();
        }));

        ButtonWidget fog = ButtonWidget.builder(Text.empty(), b -> {
            Config.fogFollowsSky = !Config.fogFollowsSky;
            b.setMessage(yesNo("Туман в цвет неба", Config.fogFollowsSky));
            Config.save();
        }).dimensions(left, y + 108, COL_W, 20).build();
        fog.setMessage(yesNo("Туман в цвет неба", Config.fogFollowsSky));
        addDrawableChild(fog);

        addDrawableChild(new ValueSlider(left, y + 132, COL_W, 20, "Скорость радуги", 0, 1, Config.rainbowSpeed, false,
                v -> Config.rainbowSpeed = (float) v));

        // ---------- Правая колонка: шляпа и частицы ----------
        ButtonWidget hatBtn = ButtonWidget.builder(Text.empty(), b -> {
            Config.hat = next(Config.hat);
            b.setMessage(Text.literal("Шляпа: " + Config.hat.label));
            Config.save();
        }).dimensions(right, y, COL_W, 20).build();
        hatBtn.setMessage(Text.literal("Шляпа: " + Config.hat.label));
        addDrawableChild(hatBtn);

        addDrawableChild(new ValueSlider(right, y + 24, COL_W, 20, "Цвет шляпы", 0, 1, Config.hatHue, false,
                v -> Config.hatHue = (float) v));

        ButtonWidget hatRainbow = ButtonWidget.builder(Text.empty(), b -> {
            Config.hatRainbow = !Config.hatRainbow;
            b.setMessage(yesNo("Радужная шляпа", Config.hatRainbow));
            Config.save();
        }).dimensions(right, y + 48, COL_W, 20).build();
        hatRainbow.setMessage(yesNo("Радужная шляпа", Config.hatRainbow));
        addDrawableChild(hatRainbow);

        ButtonWidget fxBtn = ButtonWidget.builder(Text.empty(), b -> {
            Config.fx = next(Config.fx);
            b.setMessage(Text.literal("Частицы: " + Config.fx.label));
            Config.save();
        }).dimensions(right, y + 96, COL_W, 20).build();
        fxBtn.setMessage(Text.literal("Частицы: " + Config.fx.label));
        addDrawableChild(fxBtn);

        addDrawableChild(new ValueSlider(right, y + 120, COL_W, 20, "Количество", 1, 10, Config.fxAmount, true,
                v -> Config.fxAmount = (int) v));

        // ---------- Кнопка закрыть ----------
        addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), b -> {
            Config.save();
            MinecraftClient.getInstance().setScreen(null);
        }).dimensions(this.width / 2 - 75, y + 164, 150, 20).build());
    }

    private void autoEnableSky() {
        if (Config.skyMode == Config.SkyMode.OFF) {
            Config.skyMode = Config.SkyMode.COLOR;
            if (skyButton != null) {
                skyButton.setMessage(Text.literal("Небо: " + Config.skyMode.label));
            }
        }
        Config.save();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // тёмная панель под кнопками
        ctx.fill(left - 10, top - 52, right + COL_W + 10, top + 196, 0xB0101020);

        super.render(ctx, mouseX, mouseY, delta);

        int tw = this.textRenderer.getWidth(this.title);
        ctx.drawTextWithShadow(this.textRenderer, this.title, this.width / 2 - tw / 2, top - 42, 0xFFFFD27F);

        String hint = "Right Shift - открыть/закрыть";
        int hw = this.textRenderer.getWidth(hint);
        ctx.drawTextWithShadow(this.textRenderer, hint, this.width / 2 - hw / 2, top - 30, 0xFFAAAAAA);

        ctx.drawTextWithShadow(this.textRenderer, "НЕБО", left, top - 12, 0xFFFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, "ШЛЯПА", right, top - 12, 0xFFFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, "ЧАСТИЦЫ", right, top + 82, 0xFFFFFFFF);

        // предпросмотр цвета неба
        ctx.fill(left, top + 96, left + COL_W, top + 104, Config.skyColor());
    }

    /** Ползунок со значением в подписи. */
    private static final class ValueSlider extends SliderWidget {
        private final String label;
        private final double min;
        private final double max;
        private final boolean integer;
        private final DoubleConsumer onChange;

        ValueSlider(int x, int y, int w, int h, String label, double min, double max, double current,
                    boolean integer, DoubleConsumer onChange) {
            super(x, y, w, h, Text.empty(), (current - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.integer = integer;
            this.onChange = onChange;
            updateMessage();
        }

        private double real() {
            return min + this.value * (max - min);
        }

        @Override
        protected void updateMessage() {
            if (label == null) return;
            double v = real();
            String s = integer ? String.valueOf(Math.round(v)) : String.format("%.2f", v);
            setMessage(Text.literal(label + ": " + s));
        }

        @Override
        protected void applyValue() {
            double v = real();
            onChange.accept(integer ? Math.round(v) : v);
            Config.save();
        }
    }
}
