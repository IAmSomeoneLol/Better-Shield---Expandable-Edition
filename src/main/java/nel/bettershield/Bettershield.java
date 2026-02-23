package nel.bettershield;

import nel.bettershield.effect.StunStatusEffect;
import nel.bettershield.entity.ThrownShieldEntity;
import nel.bettershield.item.ModShieldItem;
import nel.bettershield.registry.BetterShieldEnchantments;
import nel.bettershield.registry.BetterShieldItems;
import nel.bettershield.registry.BetterShieldLootModifier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.entity.Entity;

import java.util.logging.Logger;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Bettershield implements ModInitializer {
	public static final String MOD_ID = "bettershield";
	public static final Logger LOGGER = Logger.getLogger(MOD_ID);

	public static final StatusEffect STUN_EFFECT = new StunStatusEffect();

	public static final SimpleParticleType STUN_STAR_PARTICLE = FabricParticleTypes.simple();
	public static final SimpleParticleType SPARK_PARTICLE = FabricParticleTypes.simple();
	public static final SimpleParticleType CLOUD_PARTICLE = FabricParticleTypes.simple();

	public static final EntityType<ThrownShieldEntity> THROWN_SHIELD_ENTITY_TYPE = Registry.register(
			Registries.ENTITY_TYPE,
			Identifier.of(MOD_ID, "thrown_shield"),
			FabricEntityTypeBuilder.<ThrownShieldEntity>create(SpawnGroup.MISC, ThrownShieldEntity::new)
					.dimensions(EntityDimensions.fixed(1.2f, 1.2f))
					.trackRangeBlocks(80).trackedUpdateRate(10)
					.build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "thrown_shield")))
	);

	public static final HashMap<UUID, Long> PARRY_DEBOUNCE = new HashMap<>();
	public static final HashMap<UUID, Long> BASH_COOLDOWN = new HashMap<>();
	public static final HashMap<UUID, Long> SLAM_COOLDOWN = new HashMap<>();
	public static final HashMap<UUID, Long> PARRY_MELEE_COOLDOWN = new HashMap<>();
	public static final HashMap<UUID, Long> PARRY_PROJECTILE_COOLDOWN = new HashMap<>();
	public static final HashMap<UUID, Long> THROW_COOLDOWN = new HashMap<>();

	public static final HashMap<UUID, Integer> BASH_MAX = new HashMap<>();
	public static final HashMap<UUID, Integer> SLAM_MAX = new HashMap<>();
	public static final HashMap<UUID, Integer> PARRY_MELEE_MAX = new HashMap<>();
	public static final HashMap<UUID, Integer> PARRY_PROJECTILE_MAX = new HashMap<>();
	public static final HashMap<UUID, Integer> THROW_MAX = new HashMap<>();

	public static final HashMap<UUID, Double> SLAM_START_Y = new HashMap<>();

	public static BetterShieldConfig getConfig() {
		return AutoConfig.getConfigHolder(BetterShieldConfig.class).getConfig();
	}

	@Override
	public void onInitialize() {
		AutoConfig.register(BetterShieldConfig.class, JanksonConfigSerializer::new);
		BetterShieldLootModifier.register();
		nel.bettershield.registry.BetterShieldCriteria.register();

		BetterShieldItems.register();

		Registry.register(Registries.STATUS_EFFECT, Identifier.of(MOD_ID, "stun"), STUN_EFFECT);
		Registry.register(Registries.PARTICLE_TYPE, Identifier.of(MOD_ID, "stun_star"), STUN_STAR_PARTICLE);
		Registry.register(Registries.PARTICLE_TYPE, Identifier.of(MOD_ID, "spark"), SPARK_PARTICLE);
		Registry.register(Registries.PARTICLE_TYPE, Identifier.of(MOD_ID, "cloud"), CLOUD_PARTICLE);

		PayloadTypeRegistry.playC2S().register(ShieldAttackPayload.ID, ShieldAttackPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ShieldThrowPayload.ID, ShieldThrowPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(StunMobsPayload.ID, StunMobsPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SyncCooldownPayload.ID, SyncCooldownPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SlamEffectPayload.ID, SlamEffectPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(BashTrailPayload.ID, BashTrailPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ShieldThrowPayload.ID, (payload, context) -> {
			int chargeTicks = payload.chargeTicks();
			ServerPlayerEntity player = context.player();
			player.getServer().execute(() -> {
				if (player != null) {
					BetterShieldConfig config = getConfig();
					long now = player.getWorld().getTime();

					if (THROW_COOLDOWN.containsKey(player.getUuid())) {
						if (now < THROW_COOLDOWN.get(player.getUuid())) return;
					}

					ItemStack mainStack = player.getMainHandStack();
					ItemStack offStack = player.getOffHandStack();
					ItemStack shieldToThrow = null;
					boolean isOffhand = false;

					if (mainStack.getItem() instanceof net.minecraft.item.ShieldItem && EnchantmentHelper.getLevel(player.getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.LOYALTY), mainStack) > 0) {
						shieldToThrow = mainStack;
						isOffhand = false;
					} else if (offStack.getItem() instanceof net.minecraft.item.ShieldItem && EnchantmentHelper.getLevel(player.getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.LOYALTY), offStack) > 0) {
						shieldToThrow = offStack;
						isOffhand = true;
					}

					if (shieldToThrow != null) {
						int effectiveTicks = Math.max(1, chargeTicks);
						float maxChargeTicks = 20.0f;
						float ratio = Math.min(1.0f, effectiveTicks / maxChargeTicks);
						float minDmg = 0.5f;
						float maxDmg = (float) config.combat.shieldThrowDamage;
						float finalBaseDamage = minDmg + (ratio * (maxDmg - minDmg));

						ThrownShieldEntity thrownShield = new ThrownShieldEntity(player.getWorld(), player, shieldToThrow.copy());
						thrownShield.setOriginalSlot(isOffhand);
						thrownShield.setImpactDamage(finalBaseDamage);
						thrownShield.setStunEnabled(config.stunMechanics.throwStunEnabled);

						float speed = 1.0f + (ratio * 1.0f);
						thrownShield.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, speed, 1.0F);

						player.getWorld().spawnEntity(thrownShield);

						if (!player.isCreative()) {
							if (isOffhand) {
								player.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
							} else {
								player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
							}
						}

						int masterineLevel = EnchantmentHelper.getLevel(player.getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(BetterShieldEnchantments.MASTERINE), shieldToThrow);
						int baseCd = config.cooldowns.throwCooldown;
						int finalCd = (int) (baseCd * (1.0f - (masterineLevel * 0.20f)));

						triggerCooldown(player, shieldToThrow, 5, finalCd);

						float pitch = 0.8f + (ratio * 0.4f);
						player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.0F, pitch);
					}
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(ShieldAttackPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			player.getServer().execute(() -> {
				if (player != null) {
					BetterShieldConfig config = getConfig();
					ItemStack shieldStack = null;
					if (player.getMainHandStack().getItem() instanceof net.minecraft.item.ShieldItem)
						shieldStack = player.getMainHandStack();
					else if (player.getOffHandStack().getItem() instanceof net.minecraft.item.ShieldItem)
						shieldStack = player.getOffHandStack();

					if (shieldStack != null) {
						long now = player.getWorld().getTime();

						if (player.isOnGround()) {
							if (BASH_COOLDOWN.containsKey(player.getUuid())) {
								if (now < BASH_COOLDOWN.get(player.getUuid())) return;
							}

							Vec3d rotation = player.getRotationVector();
							double bashSpeed = 1.5;
							if (config.funCombos.allowVerticalBash)
								player.setVelocity(rotation.x * bashSpeed, rotation.y * bashSpeed, rotation.z * bashSpeed);
							else player.setVelocity(rotation.x * bashSpeed, 0, rotation.z * bashSpeed);
							player.velocityModified = true;

							if (player.getWorld() instanceof ServerWorld serverWorld) {
								BashTrailPayload trailPayload = new BashTrailPayload(player.getId(), 5);
								player.getWorld().getPlayers().forEach(p -> {
									if (p instanceof ServerPlayerEntity serverPlayer) {
										ServerPlayNetworking.send(serverPlayer, trailPayload);
									}
								});

								double offset = 0.5;
								double px = player.getX() - (rotation.x * offset);
								double py = player.getY() + 0.1;
								double pz = player.getZ() - (rotation.z * offset);
								serverWorld.spawnParticles(Bettershield.CLOUD_PARTICLE, px, py, pz, 5, 0.2, 0.1, 0.2, 0.02);
							}

							Vec3d pos = player.getPos().add(rotation.multiply(1.5));
							Box box = new Box(pos.x - 1, pos.y, pos.z - 1, pos.x + 1, pos.y + 2, pos.z + 1);

							int densityLevel = EnchantmentHelper.getLevel(player.getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(BetterShieldEnchantments.SHIELD_DENSITY), shieldStack);
							float damageMult = 1.0f + (densityLevel * 0.10f);

							if (shieldStack.getItem() instanceof ModShieldItem modShield) {
								damageMult += modShield.getDamageBonus();
							}

							float baseDamage = (float) config.combat.bashDamage;
							float finalDamage = baseDamage * damageMult;

							List<Entity> targets = player.getWorld().getOtherEntities(player, box);
							for (Entity target : targets) {
								if (target instanceof LivingEntity living) {
									living.damage((ServerWorld) player.getWorld(), player.getDamageSources().playerAttack(player), finalDamage);
									double knockbackStrength = config.combat.bashKnockback;
									living.takeKnockback(knockbackStrength, player.getX() - living.getX(), player.getZ() - living.getZ());

									if (config.stunMechanics.bashStunEnabled) {
										var stunEntry = Registries.STATUS_EFFECT.getEntry(Bettershield.STUN_EFFECT);
										living.addStatusEffect(new StatusEffectInstance(stunEntry, config.stunMechanics.stunDuration, 0, false, false, true));

										StunMobsPayload stunPayload = new StunMobsPayload(living.getId(), config.stunMechanics.stunDuration);
										player.getWorld().getPlayers().forEach(p -> ServerPlayNetworking.send((ServerPlayerEntity) p, stunPayload));
									}
								}
							}

							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 0.5f);

							int masterineLevel = EnchantmentHelper.getLevel(player.getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(BetterShieldEnchantments.MASTERINE), shieldStack);
							int baseCd = config.cooldowns.bashCooldown;
							int finalCd = (int) (baseCd * (1.0f - (masterineLevel * 0.20f)));

							triggerCooldown(player, shieldStack, 1, finalCd);

						} else {
							if (player.isTouchingWater()) return;
							if (SLAM_COOLDOWN.containsKey(player.getUuid())) {
								if (now < SLAM_COOLDOWN.get(player.getUuid())) return;
							}

							double minHeight = config.combat.slamMinimumHeight;
							Vec3d start = player.getEyePos();
							Vec3d end = start.add(0, -(minHeight + 0.5), 0);
							RaycastContext rcContext = new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player);
							if (player.getWorld().raycast(rcContext).getType() != HitResult.Type.MISS) return;

							player.setVelocity(0, -3.0, 0);
							player.velocityModified = true;
							player.fallDistance = 0;

							SLAM_START_Y.put(player.getUuid(), player.getY());

							int masterineLevel = EnchantmentHelper.getLevel(player.getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(BetterShieldEnchantments.MASTERINE), shieldStack);
							int baseCd = config.cooldowns.slamCooldown;
							int finalCd = (int) (baseCd * (1.0f - (masterineLevel * 0.20f)));

							triggerCooldown(player, shieldStack, 2, finalCd);
						}
					}
				}
			});
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID uuid = handler.player.getUuid();
			PARRY_DEBOUNCE.remove(uuid);
			BASH_COOLDOWN.remove(uuid);
			SLAM_COOLDOWN.remove(uuid);
			PARRY_MELEE_COOLDOWN.remove(uuid);
			PARRY_PROJECTILE_COOLDOWN.remove(uuid);
			THROW_COOLDOWN.remove(uuid);

			BASH_MAX.remove(uuid);
			SLAM_MAX.remove(uuid);
			PARRY_MELEE_MAX.remove(uuid);
			PARRY_PROJECTILE_MAX.remove(uuid);
			THROW_MAX.remove(uuid);

			SLAM_START_Y.remove(uuid);
		});
	}

	public static void triggerCooldown(PlayerEntity player, ItemStack shieldStack, int type, int ticks) {
		float reduction = 0.0f;

		if (shieldStack != null && !shieldStack.isEmpty() && shieldStack.getItem() instanceof ModShieldItem modShield) {
			reduction = modShield.getCooldownReduction();
		}
		else if (player.getActiveItem().getItem() instanceof ModShieldItem modShield) {
			reduction = modShield.getCooldownReduction();
		} else if ((player.getMainHandStack().getItem() instanceof ModShieldItem modShield)) {
			reduction = modShield.getCooldownReduction();
		} else if ((player.getOffHandStack().getItem() instanceof ModShieldItem modShield)) {
			reduction = modShield.getCooldownReduction();
		}

		int finalTicks = (int) (ticks * (1.0f - reduction));
		long expiry = player.getWorld().getTime() + finalTicks;

		if (type == 1) { BASH_COOLDOWN.put(player.getUuid(), expiry); BASH_MAX.put(player.getUuid(), finalTicks); }
		if (type == 2) { SLAM_COOLDOWN.put(player.getUuid(), expiry); SLAM_MAX.put(player.getUuid(), finalTicks); }
		if (type == 3) { PARRY_MELEE_COOLDOWN.put(player.getUuid(), expiry); PARRY_MELEE_MAX.put(player.getUuid(), finalTicks); }
		if (type == 4) { PARRY_PROJECTILE_COOLDOWN.put(player.getUuid(), expiry); PARRY_PROJECTILE_MAX.put(player.getUuid(), finalTicks); }
		if (type == 5) { THROW_COOLDOWN.put(player.getUuid(), expiry); THROW_MAX.put(player.getUuid(), finalTicks); }

		SyncCooldownPayload payload = new SyncCooldownPayload(type, finalTicks);
		ServerPlayNetworking.send((ServerPlayerEntity) player, payload);
	}

	public static boolean isParryDebounced(PlayerEntity player) {
		if (!PARRY_DEBOUNCE.containsKey(player.getUuid())) return false;
		long lastParry = PARRY_DEBOUNCE.get(player.getUuid());
		return (player.getWorld().getTime() - lastParry) < 10;
	}

	public static void setParryDebounce(PlayerEntity player) {
		PARRY_DEBOUNCE.put(player.getUuid(), player.getWorld().getTime());
	}

	public record ShieldAttackPayload() implements CustomPayload {
		public static final Id<ShieldAttackPayload> ID = new Id<>(Identifier.of(Bettershield.MOD_ID, "shield_attack"));
		public static final PacketCodec<RegistryByteBuf, ShieldAttackPayload> CODEC = PacketCodec.unit(new ShieldAttackPayload());
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record ShieldThrowPayload(int chargeTicks) implements CustomPayload {
		public static final Id<ShieldThrowPayload> ID = new Id<>(Identifier.of(Bettershield.MOD_ID, "shield_throw"));
		public static final PacketCodec<RegistryByteBuf, ShieldThrowPayload> CODEC = PacketCodec.of(
				(payload, buf) -> buf.writeInt(payload.chargeTicks()),
				buf -> new ShieldThrowPayload(buf.readInt())
		);
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record StunMobsPayload(int entityId, int duration) implements CustomPayload {
		public static final Id<StunMobsPayload> ID = new Id<>(Identifier.of(Bettershield.MOD_ID, "stun_mobs"));
		public static final PacketCodec<RegistryByteBuf, StunMobsPayload> CODEC = PacketCodec.of(
				(payload, buf) -> {
					buf.writeInt(payload.entityId());
					buf.writeInt(payload.duration());
				},
				buf -> new StunMobsPayload(buf.readInt(), buf.readInt())
		);
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record SyncCooldownPayload(int type, int duration) implements CustomPayload {
		public static final Id<SyncCooldownPayload> ID = new Id<>(Identifier.of(Bettershield.MOD_ID, "sync_cooldown"));
		public static final PacketCodec<RegistryByteBuf, SyncCooldownPayload> CODEC = PacketCodec.of(
				(payload, buf) -> {
					buf.writeInt(payload.type());
					buf.writeInt(payload.duration());
				},
				buf -> new SyncCooldownPayload(buf.readInt(), buf.readInt())
		);
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record SlamEffectPayload(double x, double y, double z, String blockId) implements CustomPayload {
		public static final Id<SlamEffectPayload> ID = new Id<>(Identifier.of(Bettershield.MOD_ID, "slam_effect"));
		public static final PacketCodec<RegistryByteBuf, SlamEffectPayload> CODEC = PacketCodec.of(
				(payload, buf) -> {
					buf.writeDouble(payload.x());
					buf.writeDouble(payload.y());
					buf.writeDouble(payload.z());
					buf.writeString(payload.blockId());
				},
				buf -> new SlamEffectPayload(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readString())
		);
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record BashTrailPayload(int entityId, int duration) implements CustomPayload {
		public static final Id<BashTrailPayload> ID = new Id<>(Identifier.of(Bettershield.MOD_ID, "bash_trail"));
		public static final PacketCodec<RegistryByteBuf, BashTrailPayload> CODEC = PacketCodec.of(
				(payload, buf) -> {
					buf.writeInt(payload.entityId());
					buf.writeInt(payload.duration());
				},
				buf -> new BashTrailPayload(buf.readInt(), buf.readInt())
		);
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}
}