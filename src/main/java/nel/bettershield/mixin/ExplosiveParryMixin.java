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
import net.minecraft.nbt.NbtCompound; // Import NBT
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
        ProjectileEntity self = (ProjectileEntity) (Object) this;

        if (self.getWorld().isClient) return;

        if (hitResult.getType() == HitResult.Type.ENTITY && ((EntityHitResult)hitResult).getEntity() instanceof PlayerEntity player) {

            // CHECK PARRY CONDITIONS
            if (player.isBlocking()) {
                BetterShieldConfig config = Bettershield.getConfig();
                ItemStack activeShield = player.getActiveItem();

                if (activeShield.isEmpty()) activeShield = player.getMainHandStack();

                int levelParryful = EnchantmentHelper.getLevel(BetterShieldEnchantments.PARRYFUL, activeShield);
                int parryWindow = config.combat.parryWindow + (levelParryful * 4);

                if (player.getItemUseTime() <= parryWindow) {

                    if (Bettershield.isParryDebounced(player)) {
                        return;
                    }

                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        BetterShieldCriteria.PARRY.trigger(serverPlayer);
                    }

                    int levelDoctrine = EnchantmentHelper.getLevel(BetterShieldEnchantments.PARRY_DOCTRINE, activeShield);
                    if (levelDoctrine > 0) {
                        int dmg = activeShield.getDamage();
                        int repair = (int) (activeShield.getMaxDamage() * (0.03f * levelDoctrine));
                        activeShield.setDamage(Math.max(0, dmg - repair));
                    }

                    if (config.stunMechanics.deflectStunEnabled && self.getOwner() instanceof LivingEntity shooter) {
                        shooter.addStatusEffect(new StatusEffectInstance(Bettershield.STUN_EFFECT, config.stunMechanics.stunDuration, 0, false, false, true));
                        PacketByteBuf buf = PacketByteBufs.create();
                        buf.writeInt(shooter.getId());
                        buf.writeInt(config.stunMechanics.stunDuration);
                        ((ServerWorld)player.getWorld()).getChunkManager().sendToNearbyPlayers(shooter, ServerPlayNetworking.createS2CPacket(Bettershield.PACKET_STUN_MOBS, buf));
                    }

                    // --- REFLECTION LOGIC (NBT METHOD - CRASH FIX) ---
                    Vec3d dir = player.getRotationVector();
                    ProjectileEntity newProj = null;

                    if (self instanceof FireballEntity oldBall) {
                        // 1. Write old data to NBT tag
                        NbtCompound nbt = new NbtCompound();
                        oldBall.writeCustomDataToNbt(nbt);

                        // 2. Read power safely (Default to 1 if missing)
                        // Vanilla key for fireball strength is "ExplosionPower" or "Power" depending on version,
                        // but 1.20 usually maps to "ExplosionPower" in NBT structure.
                        // We check both to be safe, or just instantiate a standard one.
                        int power = 1;
                        if (nbt.contains("ExplosionPower")) {
                            power = nbt.getInt("ExplosionPower");
                        } else if (nbt.contains("Power", 9)) { // List tag 9
                            // "Power" is often the acceleration vector, not explosion size.
                            // Standard ghast ball is power 1.
                            power = 1;
                        }

                        // 3. Create new
                        newProj = new FireballEntity(player.getWorld(), player, dir.x, dir.y, dir.z, power);
                    }
                    else if (self instanceof SmallFireballEntity) {
                        newProj = new SmallFireballEntity(player.getWorld(), player, dir.x, dir.y, dir.z);
                    }
                    else if (self instanceof WitherSkullEntity oldSkull) {
                        WitherSkullEntity newSkull = new WitherSkullEntity(player.getWorld(), player, dir.x, dir.y, dir.z);
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

                    int levelMasterine = EnchantmentHelper.getLevel(BetterShieldEnchantments.MASTERINE, activeShield);
                    int baseCd = config.cooldowns.parryProjectileCooldown;
                    int finalCd = (int) (baseCd * (1.0f - (levelMasterine * 0.2f)));

                    Bettershield.triggerCooldown(player, activeShield, 4, finalCd);

                    Bettershield.setParryDebounce(player);
                    player.getItemCooldownManager().set(activeShield.getItem(), 0);

                    activeShield.damage(1, player, (p) -> p.sendToolBreakStatus(player.getActiveHand()));
                    player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 1.5f);

                    self.discard();
                    ci.cancel();
                }
            }
        }
    }
}