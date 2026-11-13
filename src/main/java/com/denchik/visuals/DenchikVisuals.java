package com.denchik.visuals;

import net.fabricmc.api.ClientModInitializer;

public class DenchikVisuals implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.load();
    }
}
