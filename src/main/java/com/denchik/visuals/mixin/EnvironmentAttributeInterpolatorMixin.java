package com.denchik.visuals.mixin;

import com.denchik.visuals.Config;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeInterpolator;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Подменяет цвет неба (и при желании тумана) на тот, что выбран в меню. */
@Mixin(EnvironmentAttributeInterpolator.class)
public abstract class EnvironmentAttributeInterpolatorMixin {

    @Inject(method = "get", at = @At("RETURN"), cancellable = true)
    private void denchikvisuals$overrideSky(EnvironmentAttribute<?> attribute, float tickProgress,
                                            CallbackInfoReturnable<Object> cir) {
        if (Config.skyMode == Config.SkyMode.OFF) return;

        if (attribute == EnvironmentAttributes.SKY_COLOR_VISUAL
                || (Config.fogFollowsSky && attribute == EnvironmentAttributes.FOG_COLOR_VISUAL)) {
            cir.setReturnValue(Config.skyColor());
        }
    }
}
