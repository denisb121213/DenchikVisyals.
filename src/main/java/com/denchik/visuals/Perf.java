package com.denchik.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;

/** Готовые наборы настроек графики: для слабых, средних и мощных компьютеров. */
public final class Perf {

    private Perf() {}

    /** Ставит значение перечисления по имени (не зависит от названия самого типа). */
    private static <E extends Enum<E>> void setByName(SimpleOption<E> option, String name) {
        E[] all = option.getValue().getDeclaringClass().getEnumConstants();
        for (E e : all) {
            if (e.name().equals(name)) {
                option.setValue(e);
                return;
            }
        }
    }

    /** level: 0 - слабый ПК, 1 - средний, 2 - мощный (красиво). */
    public static void apply(MinecraftClient c, int level) {
        try {
            GameOptions o = c.options;
            switch (level) {
                case 0 -> {
                    o.getViewDistance().setValue(6);
                    o.getSimulationDistance().setValue(5);
                    o.getCloudRenderMode().setValue(CloudRenderMode.OFF);
                    setByName(o.getParticles(), "MINIMAL");
                    o.getEntityDistanceScaling().setValue(0.5);
                    o.getBiomeBlendRadius().setValue(0);
                    o.getEntityShadows().setValue(false);
                    o.getAo().setValue(false);
                    o.getWeatherRadius().setValue(3);
                    o.getVignette().setValue(false);
                    o.getCutoutLeaves().setValue(false);
                    o.getMaxFps().setValue(60);
                    Config.ecoMode = true;
                }
                case 1 -> {
                    o.getViewDistance().setValue(10);
                    o.getSimulationDistance().setValue(8);
                    o.getCloudRenderMode().setValue(CloudRenderMode.FAST);
                    setByName(o.getParticles(), "DECREASED");
                    o.getEntityDistanceScaling().setValue(0.75);
                    o.getBiomeBlendRadius().setValue(1);
                    o.getEntityShadows().setValue(true);
                    o.getAo().setValue(true);
                    o.getWeatherRadius().setValue(5);
                    o.getVignette().setValue(false);
                    o.getCutoutLeaves().setValue(false);
                    o.getMaxFps().setValue(120);
                }
                default -> {
                    o.getViewDistance().setValue(12);
                    o.getSimulationDistance().setValue(12);
                    o.getCloudRenderMode().setValue(CloudRenderMode.FANCY);
                    setByName(o.getParticles(), "ALL");
                    o.getEntityDistanceScaling().setValue(1.0);
                    o.getBiomeBlendRadius().setValue(2);
                    o.getEntityShadows().setValue(true);
                    o.getAo().setValue(true);
                    o.getWeatherRadius().setValue(10);
                    o.getVignette().setValue(true);
                    o.getCutoutLeaves().setValue(true);
                    o.getMaxFps().setValue(120);
                    Config.ecoMode = false;
                }
            }
            o.write();
            Config.save();
        } catch (Exception ignored) {
        }
    }
}
