package com.dragonegg.client;

import com.dragonegg.DragonEggMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ParrotRenderer;

public class DragonEggClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Cuerpo de prueba: el render del loro (cambiar luego por el modelo de dragón)
		EntityRendererRegistry.register(DragonEggMod.DRAGON, ParrotRenderer::new);

		DragonEggMod.OPEN_SCREEN = () -> Minecraft.getInstance().setScreenAndShow((new ElementScreen());
	}
}
