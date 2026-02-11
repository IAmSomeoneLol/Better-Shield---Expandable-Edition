package nel.bettershield;

import nel.bettershield.effect.StunStatusEffect;
import nel.bettershield.entity.ThrownShieldEntity;
import nel.bettershield.registry.BetterShieldEnchantments;
import nel.bettershield.registry.BetterShieldLootModifier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.minecraft.network.PacketByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Bettershield implements ModInitializer {
	public static final String MOD_ID = "bettershield";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final StatusEffect STUN_EFFECT = new StunStatusEffect();

	public static final Identifier PACKET_SHIELD_ATTACK = new Identifier(MOD_ID, "shield_attack");
	public static final Identifier PACKET_SHIELD_THROW = new Identifier(MOD_ID, "shield_throw");
	public static final Identifier PACKET_STUN_MOBS = new Identifier(MOD_ID, "stun_mobs");
	public static final Identifier PACKET_SYNC_COOLDOWN = new Identifier(MOD_ID, "sync_cooldown");
	public static final Identifier PACKET_SLAM_EFFECT = new Identifier(MOD_ID, "slam_effect");
	public static final Identifier PACKET_BASH_TRAIL = new Identifier(MOD_ID, "bash_trail");

	public static final DefaultParticleType STUN_STAR_PARTICLE = FabricParticleTypes.simple();
	public static final DefaultParticleType SPARK_PARTICLE = FabricParticleTypes.simple();
	public static final DefaultParticleType CLOUD_PARTICLE = FabricParticleTypes.simple(); // NEW CLOUD PARTICLE

	public static final EntityType<ThrownShieldEntity> THROWN_SHIELD_ENTITY_TYPE = Registry.register(
			Registries.ENTITY_TYPE,
			new Identifier(MOD_ID, "thrown_shield"),
			FabricEntityTypeBuilder.<ThrownShieldEntity>create(SpawnGroup.MISC, ThrownShieldEntity::new)
					.dimensions(EntityDimensions.fixed(1.2f, 1.2f))
					.trackRangeBlocks(80).trackedUpdateRate(10)
					.build()
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
		BetterShieldEnchantments.register();
		BetterShieldLootModifier.register();
		nel.bettershield.registry.BetterShieldCriteria.register();

		Registry.register(Registries.STATUS_EFFECT, new Identifier(MOD_ID, "stun"), STUN_EFFECT);
		Registry.register(Registries.PARTICLE_TYPE, new Identifier(MOD_ID, "stun_star"), STUN_STAR_PARTICLE);
		Registry.register(Registries.PARTICLE_TYPE, new Identifier(MOD_ID, "spark"), SPARK_PARTICLE);
		Registry.register(Registries.PARTICLE_TYPE, new Identifier(MOD_ID, "cloud"), CLOUD_PARTICLE); // REGISTER CLOUD

		// --- SHIELD THROW ---
		ServerPlayNetworking.registerGlobalReceiver(PACKET_SHIELD_THROW, (server, player, handler, buf, responseSender) -> {
			int chargeTicks = buf.readInt();
			server.execute(() -> {
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

					if (mainStack.isOf(Items.SHIELD) && EnchantmentHelper.getLevel(Enchantments.LOYALTY, mainStack) > 0) {
						shieldToThrow = mainStack;
						isOffhand = false;
					}
					else if (offStack.isOf(Items.SHIELD) && EnchantmentHelper.getLevel(Enchantments.LOYALTY, offStack) > 0) {
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

						int masterineLevel = EnchantmentHelper.getLevel(BetterShieldEnchantments.MASTERINE, shieldToThrow);
						int baseCd = config.cooldowns.throwCooldown;
						int finalCd = (int) (baseCd * (1.0f - (masterineLevel * 0.20f)));
						triggerCooldown(player, 5, finalCd);

						float pitch = 0.8f + (ratio * 0.4f);
						player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.0F, pitch);
					}
				}
			});
		});

		// --- BASH & SLAM ---
		ServerPlayNetworking.registerGlobalReceiver(PACKET_SHIELD_ATTACK, (server, player, handler, buf, responseSender) -> {
			server.execute(() -> {
				if (player != null) {
					BetterShieldConfig config = getConfig();
					ItemStack shieldStack = null;
					if (player.getMainHandStack().isOf(Items.SHIELD)) shieldStack = player.getMainHandStack();
					else if (player.getOffHandStack().isOf(Items.SHIELD)) shieldStack = player.getOffHandStack();

					if (shieldStack != null) {
						long now = player.getWorld().getTime();

						if (player.isOnGround()) {
							if (BASH_COOLDOWN.containsKey(player.getUuid())) {
								if (now < BASH_COOLDOWN.get(player.getUuid())) return;
							}

							Vec3d rotation = player.getRotationVector();
							double bashSpeed = 1.5;
							if (config.funCombos.allowVerticalBash) player.setVelocity(rotation.x * bashSpeed, rotation.y * bashSpeed, rotation.z * bashSpeed);
							else player.setVelocity(rotation.x * bashSpeed, 0, rotation.z * bashSpeed);
							player.velocityModified = true;

							if (player.getWorld() instanceof ServerWorld serverWorld) {
								// 1. Send Trail Packet (For lingering effect)
								PacketByteBuf trailBuf = PacketByteBufs.create();
								trailBuf.writeInt(player.getId());
								trailBuf.writeInt(5);
								player.getWorld().getPlayers().forEach(p -> {
									if (p instanceof ServerPlayerEntity serverPlayer) {
										ServerPlayNetworking.send(serverPlayer, PACKET_BASH_TRAIL, trailBuf);
									}
								});

								// 2. Initial Burst (Server Side - Immediate Feedback)
								double offset = 0.5;
								double px = player.getX() - (rotation.x * offset);
								double py = player.getY() + 0.1;
								double pz = player.getZ() - (rotation.z * offset);

								serverWorld.spawnParticles(Bettershield.CLOUD_PARTICLE,
										px, py, pz,
										5,             // Count
										0.2, 0.1, 0.2, // Spread
										0.02           // Speed
								);
							}

							Vec3d pos = player.getPos().add(rotation.multiply(1.5));
							Box box = new Box(pos.x - 1, pos.y, pos.z - 1, pos.x + 1, pos.y + 2, pos.z + 1);

							int densityLevel = EnchantmentHelper.getLevel(BetterShieldEnchantments.SHIELD_DENSITY, shieldStack);
							float damageMult = 1.0f + (densityLevel * 0.10f);
							float baseDamage = (float) config.combat.bashDamage;
							float finalDamage = baseDamage * damageMult;

							List<Entity> targets = player.getWorld().getOtherEntities(player, box);
							for (Entity target : targets) {
								if (target instanceof LivingEntity living) {
									living.damage(player.getDamageSources().playerAttack(player), finalDamage);
									double knockbackStrength = config.combat.bashKnockback;
									living.takeKnockback(knockbackStrength, player.getX() - living.getX(), player.getZ() - living.getZ());

									if (config.stunMechanics.bashStunEnabled) {
										living.addStatusEffect(new StatusEffectInstance(STUN_EFFECT, config.stunMechanics.stunDuration, 0, false, false, true));
										PacketByteBuf stunBuf = PacketByteBufs.create();
										stunBuf.writeInt(living.getId());
										stunBuf.writeInt(config.stunMechanics.stunDuration);
										player.getWorld().getPlayers().forEach(p -> ServerPlayNetworking.send((net.minecraft.server.network.ServerPlayerEntity)p, PACKET_STUN_MOBS, stunBuf));
									}
								}
							}
							player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 0.5f);

							int masterineLevel = EnchantmentHelper.getLevel(BetterShieldEnchantments.MASTERINE, shieldStack);
							int baseCd = config.cooldowns.bashCooldown;
							int finalCd = (int) (baseCd * (1.0f - (masterineLevel * 0.20f)));
							triggerCooldown(player, 1, finalCd);

						} else {
							if (player.isTouchingWater()) return;
							if (SLAM_COOLDOWN.containsKey(player.getUuid())) {
								if (now < SLAM_COOLDOWN.get(player.getUuid())) return;
							}

							double minHeight = config.combat.slamMinimumHeight;
							Vec3d start = player.getEyePos();
							Vec3d end = start.add(0, -(minHeight + 0.5), 0);
							RaycastContext context = new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player);
							if (player.getWorld().raycast(context).getType() != HitResult.Type.MISS) return;

							player.setVelocity(0, -3.0, 0);
							player.velocityModified = true;
							player.fallDistance = 0;

							SLAM_START_Y.put(player.getUuid(), player.getY());
							int masterineLevel = EnchantmentHelper.getLevel(BetterShieldEnchantments.MASTERINE, shieldStack);
							int baseCd = config.cooldowns.slamCooldown;
							int finalCd = (int) (baseCd * (1.0f - (masterineLevel * 0.20f)));
							triggerCooldown(player, 2, finalCd);
						}
					}
				}
			});
		});
	}

	public static void triggerCooldown(PlayerEntity player, int type, int ticks) {
		long expiry = player.getWorld().getTime() + ticks;
		if (type == 1) { BASH_COOLDOWN.put(player.getUuid(), expiry); BASH_MAX.put(player.getUuid(), ticks); }
		if (type == 2) { SLAM_COOLDOWN.put(player.getUuid(), expiry); SLAM_MAX.put(player.getUuid(), ticks); }
		if (type == 3) { PARRY_MELEE_COOLDOWN.put(player.getUuid(), expiry); PARRY_MELEE_MAX.put(player.getUuid(), ticks); }
		if (type == 4) { PARRY_PROJECTILE_COOLDOWN.put(player.getUuid(), expiry); PARRY_PROJECTILE_MAX.put(player.getUuid(), ticks); }
		if (type == 5) { THROW_COOLDOWN.put(player.getUuid(), expiry); THROW_MAX.put(player.getUuid(), ticks); }

		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeInt(type);
		buf.writeInt(ticks);
		ServerPlayNetworking.send((net.minecraft.server.network.ServerPlayerEntity) player, PACKET_SYNC_COOLDOWN, buf);
	}

	public static boolean isParryDebounced(PlayerEntity player) {
		if (!PARRY_DEBOUNCE.containsKey(player.getUuid())) return false;
		long lastParry = PARRY_DEBOUNCE.get(player.getUuid());
		return (player.getWorld().getTime() - lastParry) < 10;
	}

	public static void setParryDebounce(PlayerEntity player) {
		PARRY_DEBOUNCE.put(player.getUuid(), player.getWorld().getTime());
	}
}