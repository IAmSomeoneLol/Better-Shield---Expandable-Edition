package nel.bettershield.mixin;

import nel.bettershield.BetterShieldConfig;
import nel.bettershield.Bettershield;
import nel.bettershield.registry.BetterShieldCriteria;
import nel.bettershield.registry.BetterShieldEnchantments;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
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
        // Cast 'this' to ProjectileEntity to access fields
        ProjectileEntity self = (ProjectileEntity) (Object) this;

        if (self.getWorld().isClient) return;

        // Only care if we hit an Entity
        if (hitResult.getType() == HitResult.Type.ENTITY && hitResult instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() instanceof PlayerEntity player) {

                // CHECK PARRY CONDITIONS
                if (player.isBlocking()) {
                    BetterShieldConfig config = Bettershield.getConfig();
                    ItemStack activeShield = player.getActiveItem();

                    int levelParryful = EnchantmentHelper.getLevel(BetterShieldEnchantments.PARRYFUL, activeShield);
                    int parryWindow = config.combat.parryWindow + (levelParryful * 4);

                    if (player.getItemUseTime() <= parryWindow) {

                        // --- SUCCESSFUL PARRY LOGIC ---

                        // 1. Debounce Check
                        if (Bettershield.isParryDebounced(player)) {
                            ci.cancel(); // Cancel explosion even if debounced, to prevent weirdness
                            return;
                        }

                        // 2. Trigger Advancement
                        if (player instanceof ServerPlayerEntity serverPlayer) {
                            BetterShieldCriteria.PARRY.trigger(serverPlayer);
                        }

                        // 3. Handle Shield Repair (Parry Doctrine)
                        int levelDoctrine = EnchantmentHelper.getLevel(BetterShieldEnchantments.PARRY_DOCTRINE, activeShield);
                        if (levelDoctrine > 0) {
                            int dmg = activeShield.getDamage();
                            int repair = (int) (activeShield.getMaxDamage() * (0.03f * levelDoctrine));
                            activeShield.setDamage(Math.max(0, dmg - repair));
                        }

                        // 4. Stun Mechanic
                        if (config.stunMechanics.deflectStunEnabled && self.getOwner() instanceof LivingEntity shooter) {
                            shooter.addStatusEffect(new StatusEffectInstance(Bettershield.STUN_EFFECT, config.stunMechanics.stunDuration, 0, false, false, true));
                            PacketByteBuf buf = PacketByteBufs.create();
                            buf.writeInt(shooter.getId());
                            buf.writeInt(config.stunMechanics.stunDuration);
                            ((ServerWorld)player.getWorld()).getChunkManager().sendToNearbyPlayers(shooter, ServerPlayNetworking.createS2CPacket(Bettershield.PACKET_STUN_MOBS, buf));
                        }

                        // 5. REFLECTION LOGIC (Spawn New, Kill Old)
                        Vec3d dir = player.getRotationVector();
                        ProjectileEntity newProj = null;

                        if (self instanceof FireballEntity oldBall) {
                            NbtCompound nbt = new NbtCompound();
                            oldBall.writeCustomDataToNbt(nbt);
                            int power = nbt.contains("ExplosionPower") ? nbt.getInt("ExplosionPower") : 1;
                            newProj = new FireballEntity(player.getWorld(), player, dir.x * 0.1, dir.y * 0.1, dir.z * 0.1, power);
                        }
                        else if (self instanceof SmallFireballEntity) {
                            newProj = new SmallFireballEntity(player.getWorld(), player, dir.x * 0.1, dir.y * 0.1, dir.z * 0.1);
                        }
                        else if (self instanceof WitherSkullEntity oldSkull) {
                            WitherSkullEntity newSkull = new WitherSkullEntity(player.getWorld(), player, dir.x * 0.1, dir.y * 0.1, dir.z * 0.1);
                            if (oldSkull.isCharged()) newSkull.setCharged(true);
                            newProj = newSkull;
                        }

                        if (newProj != null) {
                            newProj.addCommandTag("bettershield_reflected");
                            newProj.setOwner(player);
                            // Move 2 blocks away to prevent self-collision
                            Vec3d spawnPos = player.getEyePos().add(dir.multiply(2.0));
                            newProj.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
                            player.getWorld().spawnEntity(newProj);
                        }

                        // --- PARTICLE EFFECT (SPARKS) ---
                        if (player.getWorld() instanceof ServerWorld serverWorld) {
                            serverWorld.spawnParticles(Bettershield.SPARK_PARTICLE,
                                    player.getX(), player.getEyeY() - 0.2, player.getZ(),
                                    15, 0.4, 0.4, 0.4, 0.2);
                        }
                        // --------------------------------

                        // 6. Cooldowns & Effects
                        int levelMasterine = EnchantmentHelper.getLevel(BetterShieldEnchantments.MASTERINE, activeShield);
                        int baseCd = config.cooldowns.parryProjectileCooldown;
                        int finalCd = (int) (baseCd * (1.0f - (levelMasterine * 0.2f)));
                        Bettershield.triggerCooldown(player, 4, finalCd);

                        Bettershield.setParryDebounce(player);
                        player.getItemCooldownManager().set(Items.SHIELD, 0);
                        activeShield.damage(1, player, (p) -> p.sendToolBreakStatus(player.getActiveHand()));
                        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 1.5f);

                        // 7. CRITICAL: DISCARD OLD & CANCEL COLLISION
                        self.discard(); // Deletes the fireball from world
                        ci.cancel();    // STOPS the "Explode" method from running
                    }
                }
            }
        }
    }
}