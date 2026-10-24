package com.dragonegg;

import net.minecraft.world.entity.animal.parrot.Parrot;

/** Los 4 elementos. El "variant" solo sirve para que el cuerpo de prueba (loro) cambie de color. */
public enum Element {
	FIRE("fire", Parrot.Variant.RED_BLUE),
	WATER("water", Parrot.Variant.BLUE),
	PLANT("plant", Parrot.Variant.GREEN),
	ROCK("rock", Parrot.Variant.GRAY);

	public final String id;
	public final Parrot.Variant variant;

	Element(String id, Parrot.Variant variant) {
		this.id = id;
		this.variant = variant;
	}

	public static Element byIndex(int i) {
		Element[] all = values();
		return all[Math.floorMod(i, all.length)];
	}

	public static Element byVariant(Parrot.Variant v) {
		for (Element e : values()) {
			if (e.variant == v) return e;
		}
		return FIRE;
	}
}
