package com.denchik.visuals.mixin;

import com.denchik.visuals.ChestButtons;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Рисует кнопки над окном сундука. */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void denchikvisuals$chestButtons(DrawContext context, int mouseX, int mouseY, float deltaTicks,
                                             CallbackInfo ci) {
        ChestButtons.render(context, (Screen) (Object) this, mouseX, mouseY);
    }
}
