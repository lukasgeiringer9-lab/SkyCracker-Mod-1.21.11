package de.skycracker;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class SkyCrackerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRenderers.register(SkyCrackerMod.SKYCRACKER, SkyCrackerRenderer::new);
    }
}
