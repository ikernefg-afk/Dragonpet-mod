package com.dragonegg.client;

import com.dragonegg.ChooseElementPayload;
import com.dragonegg.Element;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Menú: nombre del dragón (opcional) + 4 botones: fuego, agua, planta y roca. */
public class ElementScreen extends Screen {
	private EditBox nameBox;

	public ElementScreen() {
		super(Component.translatable("screen.dragonegg.title"));
	}

	@Override
	protected void init() {
		int w = 140, h = 20, gap = 6;
		int x = this.width / 2 - w / 2;
		int top = this.height / 2 - 70;

		nameBox = new EditBox(this.font, x, top + 20, w, h, Component.translatable("screen.dragonegg.name_hint"));
		nameBox.setMaxLength(24);
		nameBox.setHint(Component.translatable("screen.dragonegg.name_hint"));
		addRenderableWidget(nameBox);

		Element[] elements = Element.values();
		for (int i = 0; i < elements.length; i++) {
			Element e = elements[i];
			addRenderableWidget(Button.builder(
					Component.translatable("element.dragonegg." + e.id),
					button -> {
						ClientPlayNetworking.send(new ChooseElementPayload(e.ordinal(), nameBox.getValue()));
						onClose();
					})
					.bounds(x, top + 55 + i * (h + gap), w, h)
					.build());
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 82, 0xFFFFFFFF);
	}
}
