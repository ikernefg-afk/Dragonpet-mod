package com.dragonegg;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Paquete cliente -> servidor: "he elegido este elemento y este nombre". */
public record ChooseElementPayload(int element, String name) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ChooseElementPayload> TYPE =
			new CustomPacketPayload.Type<>(DragonEggMod.id("choose_element"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ChooseElementPayload> CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, ChooseElementPayload::element,
					ByteBufCodecs.STRING_UTF8, ChooseElementPayload::name,
					ChooseElementPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
