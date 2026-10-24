package com.dragonegg;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** El huevo que aparece en el suelo. Al romperlo suelta el ítem (ver loot_table). */
public class ElementalEggBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 14, 12);

	public ElementalEggBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}
