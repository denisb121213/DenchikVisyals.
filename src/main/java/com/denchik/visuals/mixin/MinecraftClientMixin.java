package com.denchik.visuals.mixin;

import com.denchik.visuals.Effects;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void denchikvisuals$onTick(CallbackInfo ci) {
        Effects.tick((MinecraftClient) (Object) this);
    }
}
