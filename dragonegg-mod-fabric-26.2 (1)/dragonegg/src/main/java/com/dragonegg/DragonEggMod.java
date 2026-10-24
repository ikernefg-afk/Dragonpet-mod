package com.dragonegg;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class DragonEggMod implements ModInitializer {
	public static final String MOD_ID = "dragonegg";

	/** El cliente lo reemplaza para abrir el menú. En el servidor no hace nada. */
	public static Runnable OPEN_SCREEN = () -> {};

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	// ---- Registros ----
	public static final ResourceKey<Block> EGG_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, id("elemental_egg"));
	public static final ResourceKey<Item> EGG_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("elemental_egg"));
	public static final ResourceKey<EntityType<?>> DRAGON_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("dragon"));

	public static Block EGG_BLOCK;
	public static Item EGG_ITEM;
	public static EntityType<DragonEntity> DRAGON;

	@Override
	public void onInitialize() {
		EGG_BLOCK = Registry.register(BuiltInRegistries.BLOCK, EGG_BLOCK_KEY,
				new ElementalEggBlock(BlockBehaviour.Properties.of()
						.setId(EGG_BLOCK_KEY)
						.strength(0.8f)
						.sound(SoundType.STONE)
						.noOcclusion()));

		EGG_ITEM = Registry.register(BuiltInRegistries.ITEM, EGG_ITEM_KEY,
				new ElementalEggItem(new Item.Properties().setId(EGG_ITEM_KEY).stacksTo(1)));

		DRAGON = Registry.register(BuiltInRegistries.ENTITY_TYPE, DRAGON_KEY,
				EntityType.Builder.<DragonEntity>of(DragonEntity::new, MobCategory.CREATURE)
						.sized(0.5f, 0.9f)
						.clientTrackingRange(10)
						.build(DRAGON_KEY));
		FabricDefaultAttributeRegistry.register(DRAGON, DragonEntity.createDragonAttributes());

		// Que el huevo aparezca en el mundo (la definición está en data/dragonegg/worldgen)
		BiomeModifications.addFeature(
				BiomeSelectors.foundInOverworld(),
				GenerationStep.Decoration.SURFACE_STRUCTURES,
				ResourceKey.create(Registries.PLACED_FEATURE, id("elemental_egg")));

		// Paquete: el jugador eligió elemento
		PayloadTypeRegistry.playC2S().register(ChooseElementPayload.TYPE, ChooseElementPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ChooseElementPayload.TYPE, (payload, context) ->
				hatch(context.player(), Element.byIndex(payload.element()), payload.name()));
	}

	/** Gasta un huevo y hace nacer el dragón. */
	private static void hatch(ServerPlayer player, Element element, String name) {
		if (!(player.level() instanceof ServerLevel level)) return;

		ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
		if (!stack.is(EGG_ITEM)) {
			stack = player.getItemInHand(InteractionHand.OFF_HAND);
			if (!stack.is(EGG_ITEM)) return; // sin huevo en la mano, no hay dragón
		}
		stack.shrink(1);

		DragonEntity dragon = new DragonEntity(DRAGON, level);
		dragon.setPos(player.getX(), player.getY(), player.getZ());
		dragon.setElement(element);
		dragon.tame(player);

		String clean = name == null ? "" : name.trim();
		if (!clean.isEmpty()) {
			dragon.setCustomName(Component.literal(clean.substring(0, Math.min(clean.length(), 24))));
			dragon.setCustomNameVisible(true);
		}
		level.addFreshEntity(dragon);
	}
}
