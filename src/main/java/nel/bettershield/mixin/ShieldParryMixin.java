package nel.bettershield.mixin;

import nel.bettershield.BetterShieldConfig;
import nel.bettershield.Bettershield;
import nel.bettershield.registry.BetterShieldCriteria;
import nel.bettershield.registry.BetterShieldEnchantments;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.potion.Potion;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(LivingEntity.class)
public abstract class ShieldParryMixin {

    @Shadow public abstract boolean isBlocking();
    @Shadow public abstract int getItemUseTime();
    @Shadow public abstract ItemStack getActiveItem();

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity user = (LivingEntity) (Object) this;
        BetterShieldConfig config = Bettershield.getConfig();

        if (user instanceof PlayerEntity player && this.isBlocking()) {

            ItemStack activeShield = this.getActiveItem();

            // Safety check
            if (activeShield.isEmpty()) activeShield = player.getMainHandStack();

            int levelParryful = EnchantmentHelper.getLevel(BetterShieldEnchantments.PARRYFUL, activeShield);
            int parryWindow = config.combat.parryWindow + (levelParryful * 4);

            if (this.getItemUseTime() <= parryWindow) {

                if (Bettershield.isParryDebounced(player)) return;

                if (!player.getWorld().isClient && player instanceof ServerPlayerEntity serverPlayer) {
                    BetterShieldCriteria.PARRY.trigger(serverPlayer);
                }

                int levelDoctrine = EnchantmentHelper.getLevel(BetterShieldEnchantments.PARRY_DOCTRINE, activeShield);
                if (levelDoctrine > 0) {
                    int damage = activeShield.getDamage();
                    int maxDamage = activeShield.getMaxDamage();
                    int repairAmount = (int) (maxDamage * (0.03f * levelDoctrine));
                    activeShield.setDamage(Math.max(0, damage - repairAmount));
                }

                int levelMasterine = EnchantmentHelper.getLevel(BetterShieldEnchantments.MASTERINE, activeShield);

                // --- PROJECTILE PARRY ---
                if (source.getSource() instanceof ProjectileEntity oldProjectile &&
                        !(oldProjectile instanceof ExplosiveProjectileEntity) &&
                        !(oldProjectile instanceof SmallFireballEntity)) {

                    if (Bettershield.PARRY_PROJECTILE_COOLDOWN.containsKey(player.getUuid())) {
                        if (player.getWorld().getTime() < Bettershield.PARRY_PROJECTILE_COOLDOWN.get(player.getUuid())) {
                            return;
                        }
                    }

                    cir.setReturnValue(false);
                    Vec3d finalDirection = player.getRotationVector();

                    if (!player.getWorld().isClient) {
                        if (config.stunMechanics.deflectStunEnabled && source.getAttacker() instanceof LivingEntity shooter) {
                            shooter.addStatusEffect(new StatusEffectInstance(Bettershield.STUN_EFFECT, config.stunMechanics.stunDuration, 0, false, false, true));
                            PacketByteBuf buf = PacketByteBufs.create();
                            buf.writeInt(shooter.getId());
                            buf.writeInt(config.stunMechanics.stunDuration);
                            ((ServerWorld)player.getWorld()).getChunkManager().sendToNearbyPlayers(shooter, ServerPlayNetworking.createS2CPacket(Bettershield.PACKET_STUN_MOBS, buf));
                        }

                        ProjectileEntity newProjectile = createReflectedProjectile(oldProjectile, player, finalDirection, config.combat.arrowReflectSpeed);

                        if (newProjectile != null) {
                            newProjectile.addCommandTag("bettershield_reflected");

                            int levelDeflector = EnchantmentHelper.getLevel(BetterShieldEnchantments.DEFLECTOR, activeShield);
                            if (newProjectile instanceof PersistentProjectileEntity arrow) {
                                if (levelDeflector > 0) {
                                    double oldDmg = arrow.getDamage();
                                    arrow.setDamage(oldDmg * (1.0 + (levelDeflector * 0.15)));
                                }
                                arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
                            }
                            player.getWorld().spawnEntity(newProjectile);
                            oldProjectile.discard();
                        }

                        if (player.getWorld() instanceof ServerWorld serverWorld) {
                            serverWorld.spawnParticles(Bettershield.SPARK_PARTICLE,
                                    player.getX(), player.getEyeY() - 0.2, player.getZ(),
                                    10, 0.3, 0.3, 0.3, 0.1);
                        }

                        int baseCd = config.cooldowns.parryProjectileCooldown;
                        int finalCd = (int) (baseCd * (1.0f - (levelMasterine * 0.2f)));

                        // Pass activeShield to apply cooldown reduction
                        Bettershield.triggerCooldown(player, activeShield, 4, finalCd);
                    }

                    Bettershield.setParryDebounce(player);
                    player.getItemCooldownManager().set(activeShield.getItem(), 0);
                    this.damageShield(player, 1);
                    player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    return;
                }

                // --- MELEE PARRY ---
                if (source.getAttacker() instanceof LivingEntity attacker) {
                    if (Bettershield.PARRY_MELEE_COOLDOWN.containsKey(player.getUuid())) {
                        if (player.getWorld().getTime() < Bettershield.PARRY_MELEE_COOLDOWN.get(player.getUuid())) {
                            return;
                        }
                    }

                    cir.setReturnValue(false);

                    if (config.stunMechanics.parryStunEnabled) {
                        attacker.addStatusEffect(new StatusEffectInstance(Bettershield.STUN_EFFECT, config.stunMechanics.stunDuration, 0, false, false, true));
                    }

                    if (!player.getWorld().isClient) {
                        if (config.stunMechanics.parryStunEnabled) {
                            PacketByteBuf buf = PacketByteBufs.create();
                            buf.writeInt(attacker.getId());
                            buf.writeInt(config.stunMechanics.stunDuration);
                            ServerWorld serverWorld = (ServerWorld) player.getWorld();
                            serverWorld.getChunkManager().sendToNearbyPlayers(attacker, ServerPlayNetworking.createS2CPacket(Bettershield.PACKET_STUN_MOBS, buf));
                        }

                        if (player.getWorld() instanceof ServerWorld serverWorld) {
                            serverWorld.spawnParticles(Bettershield.SPARK_PARTICLE,
                                    player.getX(), player.getEyeY() - 0.2, player.getZ(),
                                    10, 0.3, 0.3, 0.3, 0.1);
                        }

                        int baseCd = config.cooldowns.parryMeleeCooldown;
                        int finalCd = (int) (baseCd * (1.0f - (levelMasterine * 0.2f)));

                        // Pass activeShield to apply cooldown reduction
                        Bettershield.triggerCooldown(player, activeShield, 3, finalCd);
                    }

                    Bettershield.setParryDebounce(player);
                    player.getItemCooldownManager().set(activeShield.getItem(), 0);
                    this.damageShield(player, 1);

                    player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 1.0f, 1.5f);
                    attacker.takeKnockback(0.5, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());

                    return;
                }
            }
        }
    }

    private ProjectileEntity createReflectedProjectile(ProjectileEntity old, PlayerEntity owner, Vec3d dir, double speed) {
        ServerWorld world = (ServerWorld) owner.getWorld();
        ProjectileEntity result = null;
        double spawnOffset = 0.5;

        if (old instanceof ArrowEntity oldArrow) {
            ArrowEntity newArrow = new ArrowEntity(world, owner);
            newArrow.initFromStack(new ItemStack(Items.ARROW));

            // Use the Accessor Interface below
            if (oldArrow instanceof ArrowEntityAccessor accessor) {
                Potion potion = accessor.getPotion();
                for (StatusEffectInstance effect : potion.getEffects()) {
                    newArrow.addEffect(new StatusEffectInstance(effect));
                }
                Set<StatusEffectInstance> customEffects = accessor.getCustomEffects(); // THIS WAS THE ERROR FIX
                if (customEffects != null) {
                    for (StatusEffectInstance effect : customEffects) {
                        newArrow.addEffect(new StatusEffectInstance(effect));
                    }
                }
                ((ArrowEntityAccessor) newArrow).invokeSetColor(oldArrow.getColor());
            }

            newArrow.setDamage(oldArrow.getDamage());
            newArrow.setCritical(oldArrow.isCritical());
            if (oldArrow.isOnFire()) newArrow.setOnFireFor(100);
            result = newArrow;
        }
        else if (old instanceof SpectralArrowEntity oldSpectral) {
            SpectralArrowEntity newSpectral = new SpectralArrowEntity(world, owner);
            newSpectral.setDamage(oldSpectral.getDamage());
            newSpectral.setCritical(oldSpectral.isCritical());
            if (oldSpectral.isOnFire()) newSpectral.setOnFireFor(100);
            result = newSpectral;
        }
        else if (old instanceof TridentEntity oldTrident) {
            if (oldTrident instanceof TridentEntityAccessor accessor) {
                ItemStack stack = accessor.getTridentStack();
                TridentEntity newTrident = new TridentEntity(world, owner, stack.isEmpty() ? new ItemStack(Items.TRIDENT) : stack);
                newTrident.setDamage(oldTrident.getDamage());
                result = newTrident;
            } else {
                TridentEntity newTrident = new TridentEntity(world, owner, new ItemStack(Items.TRIDENT));
                newTrident.setDamage(oldTrident.getDamage());
                result = newTrident;
            }
        }

        if (result != null) {
            Vec3d spawnPos = owner.getEyePos().add(dir.multiply(spawnOffset));
            result.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
            result.setVelocity(dir.x, dir.y, dir.z, (float) speed, 0.0f);
            result.setOwner(owner);
        }

        return result;
    }

    private void damageShield(PlayerEntity player, int amount) {
        ItemStack stack = player.getActiveItem();
        if (stack.getItem() instanceof ShieldItem) {
            stack.damage(amount, player, (p) -> p.sendToolBreakStatus(player.getActiveHand()));
        }
    }

    // --- ACCESSOR INTERFACES (Mappings Fixed) ---

    @Mixin(ArrowEntity.class)
    public interface ArrowEntityAccessor {
        @org.spongepowered.asm.mixin.gen.Accessor("potion")
        Potion getPotion();

        // FIXED: Renamed "customPotionEffects" to "effects" for 1.20+
        @org.spongepowered.asm.mixin.gen.Accessor("effects")
        Set<StatusEffectInstance> getCustomEffects();

        @org.spongepowered.asm.mixin.gen.Invoker("setColor")
        void invokeSetColor(int color);
    }

    @Mixin(TridentEntity.class)
    public interface TridentEntityAccessor {
        @org.spongepowered.asm.mixin.gen.Accessor("tridentStack")
        ItemStack getTridentStack();
    }
}