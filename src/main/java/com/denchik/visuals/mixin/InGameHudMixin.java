package com.denchik.visuals.mixin;

import com.denchik.visuals.Config;
import com.denchik.visuals.Hud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void denchikvisuals$renderHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Hud.render(context);
    }

    // Прячем стандартный прицел, если включён свой
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void denchikvisuals$crosshair(CallbackInfo ci) {
        if (Config.crosshair != Config.Crosshair.OFF) {
            ci.cancel();
        }
    }
}
