package com.dragonegg;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * El dragón. De momento es un Parrot "disfrazado" (para tener vuelo, seguir al dueño y render
 * gratis). Más adelante se cambia por un modelo de dragón hecho en Blockbench.
 *
 * - Inmortal: nunca recibe daño.
 * - Evoluciona: cría -> joven -> adulto (y ahí se queda).
 * - Mascota: se le puede poner nombre (en el menú o con una etiqueta), acariciar y dar de comer.
 * - Adulto: el dueño se puede montar y volar.
 * - Ayuda al dueño según su elemento.
 */
public class DragonEntity extends Parrot {
	// Ticks de vida para evolucionar (20 ticks = 1 segundo)
	public static final int YOUNG_AT = 20 * 60 * 20;  // 20 minutos
	public static final int ADULT_AT = 20 * 60 * 60;  // 60 minutos
	/** Cada comida adelanta la evolución 1 minuto. */
	public static final int FOOD_GROWTH = 20 * 60;

	private static final double[] SCALE_BY_STAGE = {0.6, 1.0, 1.6};
	private static final double FLY_SPEED = 0.6;

	private int dragonAge = 0;
	private int stage = -1;

	public DragonEntity(EntityType<? extends DragonEntity> type, Level level) {
		super(type, level);
	setVariant(element.variant);}

	public static AttributeSupplier.Builder createDragonAttributes() {
		return Parrot.createAttributes()
				.add(Attributes.MAX_HEALTH, 40.0)
				.add(Attributes.ATTACK_DAMAGE, 4.0);
	}

	public Element getElement() {
		return Element.byVariant(getVariant());
	}

	public void setElement(Element element) {
		setVariant(element.variant);
	}

	public int getStage() {
		return Math.max(stage, 0);
	}

	public boolean isAdult() {
		return getStage() >= 2;
	}

	// ---------- Inmortal ----------

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	/** Evita que se suba al hombro del jugador como un loro normal. */
	@Override
	public boolean canSitOnShoulder() {
		return false;
	}

	// ---------- Mascota: comer, acariciar, montar ----------

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!isOwnedBy(player)) return InteractionResult.PASS;

		ItemStack stack = player.getItemInHand(hand);
		if (stack.is(Items.COOKIE)) return InteractionResult.PASS; // las galletas envenenan a los loros

		// Dar de comer: cualquier comida, y adelanta la evolución
		if (stack.has(DataComponents.FOOD)) {
			if (level() instanceof ServerLevel sl) {
				if (!player.getAbilities().instabuild) stack.shrink(1);
				if (!isAdult()) dragonAge += FOOD_GROWTH;
				playSound(SoundEvents.PARROT_EAT, 1.0f, 1.0f);
				hearts(sl);
			}
			return InteractionResult.SUCCESS;
		}

		// Mano vacía
		if (stack.isEmpty() && hand == InteractionHand.MAIN_HAND) {
			// Adulto: montar. Con shift se acaricia.
			if (isAdult() && !player.isShiftKeyDown()) {
				if (level() instanceof ServerLevel) player.startRiding(this);
				return InteractionResult.SUCCESS;
			}
			// Acariciar
			if (level() instanceof ServerLevel sl) {
				playSound(SoundEvents.PARROT_AMBIENT, 1.0f, 1.2f);
				hearts(sl);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	private void hearts(ServerLevel sl) {
		sl.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(),
				5, 0.3, 0.3, 0.3, 0.0);
	}

	// ---------- Volar montado ----------

	@Override
	public LivingEntity getControllingPassenger() {
		if (isAdult() && getFirstPassenger() instanceof Player player) return player;
		return null;
	}

	@Override
	public void travel(Vec3 input) {
		if (isVehicle() && getControllingPassenger() instanceof Player rider) {
			// Mira donde mira el jugador
			setYRot(rider.getYRot());
			yRotO = getYRot();
			setXRot(rider.getXRot() * 0.5f);
			setRot(getYRot(), getXRot());
			yBodyRot = getYRot();
			yHeadRot = yBodyRot;

			// W = avanzar hacia donde miras (si miras arriba, subes). Sin tecla = se queda flotando.
			Vec3 motion = Vec3.ZERO;
			if (rider.zza > 0) {
				motion = rider.getLookAngle().scale(FLY_SPEED);
			} else if (rider.zza < 0) {
				motion = rider.getLookAngle().scale(-FLY_SPEED * 0.4);
			}
			setDeltaMovement(motion);
			move(MoverType.SELF, motion);
			return;
		}
		super.travel(input);
	}

	// ---------- Lógica por tick ----------

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel sl)) return;

		dragonAge++;
		updateStage();

		if (getHealth() < getMaxHealth()) setHealth(getMaxHealth());

		if (isVehicle()) getNavigation().stop(); // montado: manda el jugador, no la IA

		LivingEntity owner = getOwner();
		if (owner instanceof ServerPlayer player) {
			// Si cae al vacío o se aleja demasiado, vuelve con el dueño
			if (!isVehicle() && (getY() < sl.getMinY() || distanceToSqr(player) > 40 * 40)) {
				setPos(player.getX(), player.getY(), player.getZ());
			}
			if (tickCount % 40 == 0) helpOwner(sl, player);
			if (tickCount % 100 == 0 && getElement() == Element.PLANT) growCropsNear(sl, player);
		}
	}

	private void updateStage() {
		int newStage = dragonAge >= ADULT_AT ? 2 : dragonAge >= YOUNG_AT ? 1 : 0;
		if (newStage == stage) return;
		stage = newStage;
		AttributeInstance scale = getAttribute(Attributes.SCALE);
		if (scale != null) scale.setBaseValue(SCALE_BY_STAGE[stage]);
	}

	// ---------- Ayuda según elemento ----------

	private void helpOwner(ServerLevel sl, ServerPlayer player) {
		if (player.distanceToSqr(this) > 24 * 24) return;
		int amp = stage >= 2 ? 1 : 0;

		switch (getElement()) {
			case FIRE -> player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120, 0, true, false));
			case WATER -> {
				player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 120, 0, true, false));
				if (player.isOnFire()) player.clearFire();
			}
			case PLANT -> player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, amp, true, false));
			case ROCK -> {
				player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 120, amp, true, false));
				player.addEffect(new MobEffectInstance(MobEffects.HASTE, 120, amp, true, false));
			}
		}

		// Todos los dragones defienden al dueño de monstruos cercanos
		Monster target = sl.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(8.0))
				.stream().findFirst().orElse(null);
		if (target != null) {
			target.hurtServer(sl, damageSources().mobAttack(this), 2.0f + 2.0f * getStage());
			if (getElement() == Element.FIRE) target.igniteForSeconds(4.0f);
		}
	}

	private void growCropsNear(ServerLevel sl, ServerPlayer player) {
		for (int i = 0; i < 4; i++) {
			BlockPos pos = player.blockPosition().offset(
					sl.getRandom().nextInt(9) - 4, 0, sl.getRandom().nextInt(9) - 4);
			BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), sl, pos);
		}
	}

	// ---------- Guardado ----------

	@Override
	public void addAdditionalSaveData(ValueOutput out) {
		super.addAdditionalSaveData(out);
		out.putInt("DragonAge", dragonAge);
	}

	@Override
	public void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		dragonAge = in.getIntOr("DragonAge", 0);
	}
}
