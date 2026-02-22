package nel.bettershield.mixin;

import nel.bettershield.BetterShieldConfig;
import nel.bettershield.Bettershield;
import nel.bettershield.registry.BetterShieldCriteria;
import nel.bettershield.registry.BetterShieldEnchantments;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({FireballEntity.class, WitherSkullEntity.class, SmallFireballEntity.class})
public abstract class ExplosiveParryMixin {

    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void onCollisionParry(HitResult hitResult, CallbackInfo ci) {
        ProjectileEntity self = (ProjectileEntity) (Object) this;

        if (self.getWorld().isClient) return;

        if (hitResult.getType() == HitResult.Type.ENTITY && ((EntityHitResult)hitResult).getEntity() instanceof PlayerEntity player) {

            if (player.isBlocking()) {
                BetterShieldConfig config = Bettershield.getConfig();
                ItemStack activeShield = player.getActiveItem();

                if (activeShield.isEmpty()) activeShield = player.getMainHandStack();

                var enchantmentRegistry = player.getWorld().getRegistryManager().getWrapperOrThrow(RegistryKeys.ENCHANTMENT);

                int levelParryful = EnchantmentHelper.getLevel(enchantmentRegistry.getOrThrow(BetterShieldEnchantments.PARRYFUL), activeShield);
                int parryWindow = config.combat.parryWindow + (levelParryful * 4);

                if (player.getItemUseTime() <= parryWindow) {

                    if (Bettershield.isParryDebounced(player)) {
                        return;
                    }

                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        BetterShieldCriteria.PARRY.trigger(serverPlayer);
                    }

                    int levelDoctrine = EnchantmentHelper.getLevel(enchantmentRegistry.getOrThrow(BetterShieldEnchantments.PARRY_DOCTRINE), activeShield);
                    if (levelDoctrine > 0) {
                        int dmg = activeShield.getDamage();
                        int repair = (int) (activeShield.getMaxDamage() * (0.03f * levelDoctrine));
                        activeShield.setDamage(Math.max(0, dmg - repair));
                    }

                    if (config.stunMechanics.deflectStunEnabled && self.getOwner() instanceof LivingEntity shooter) {
                        var stunEntry = Registries.STATUS_EFFECT.getEntry(Bettershield.STUN_EFFECT);
                        shooter.addStatusEffect(new StatusEffectInstance(stunEntry, config.stunMechanics.stunDuration, 0, false, false, true));

                        Bettershield.StunMobsPayload stunPayload = new Bettershield.StunMobsPayload(shooter.getId(), config.stunMechanics.stunDuration);
                        player.getWorld().getPlayers().forEach(p -> ServerPlayNetworking.send((ServerPlayerEntity)p, stunPayload));
                    }

                    Vec3d dir = player.getRotationVector();
                    ProjectileEntity newProj = null;

                    if (self instanceof FireballEntity oldBall) {
                        NbtCompound nbt = new NbtCompound();
                        oldBall.writeCustomDataToNbt(nbt);

                        int power = 1;
                        if (nbt.contains("ExplosionPower")) {
                            power = nbt.getInt("ExplosionPower");
                        } else if (nbt.contains("Power", 9)) {
                            power = 1;
                        }

                        newProj = new FireballEntity(player.getWorld(), player, dir.normalize(), power);
                    }
                    else if (self instanceof SmallFireballEntity) {
                        newProj = new SmallFireballEntity(player.getWorld(), player, dir.normalize());
                    }
                    else if (self instanceof WitherSkullEntity oldSkull) {
                        WitherSkullEntity newSkull = new WitherSkullEntity(player.getWorld(), player, dir.normalize());
                        if (oldSkull.isCharged()) newSkull.setCharged(true);
                        newProj = newSkull;
                    }

                    if (newProj != null) {
                        Vec3d spawnPos = player.getEyePos().add(dir.multiply(1.5));
                        newProj.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, player.getYaw(), player.getPitch());
                        player.getWorld().spawnEntity(newProj);
                    }

                    if (player.getWorld() instanceof ServerWorld serverWorld) {
                        serverWorld.spawnParticles(Bettershield.SPARK_PARTICLE,
                                player.getX(), player.getEyeY() - 0.2, player.getZ(),
                                15, 0.4, 0.4, 0.4, 0.2);
                    }

                    int levelMasterine = EnchantmentHelper.getLevel(enchantmentRegistry.getOrThrow(BetterShieldEnchantments.MASTERINE), activeShield);
                    int baseCd = config.cooldowns.parryProjectileCooldown;
                    int finalCd = (int) (baseCd * (1.0f - (levelMasterine * 0.2f)));

                    Bettershield.triggerCooldown(player, activeShield, 4, finalCd);
                    Bettershield.setParryDebounce(player);
                    player.getItemCooldownManager().set(activeShield.getItem(), 0);

                    activeShield.damage(1, player, player.getActiveHand() == Hand.MAIN_HAND ? net.minecraft.entity.EquipmentSlot.MAINHAND : net.minecraft.entity.EquipmentSlot.OFFHAND);
                    player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 1.5f);

                    self.discard();
                    ci.cancel();
                }
            }
        }
    }
}