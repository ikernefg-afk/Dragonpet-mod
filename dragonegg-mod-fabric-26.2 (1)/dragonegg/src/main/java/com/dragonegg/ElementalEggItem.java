package com.dragonegg;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** El huevo en la mano. Clic derecho = abre el menú de elementos (solo en el cliente). */
public class ElementalEggItem extends Item {
	public ElementalEggItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			DragonEggMod.OPEN_SCREEN.run();
		}
		return InteractionResult.SUCCESS;
	}
}
