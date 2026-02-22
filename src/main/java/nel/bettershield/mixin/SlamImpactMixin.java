package nel.bettershield.mixin;

import nel.bettershield.BetterShieldConfig;
import nel.bettershield.Bettershield;
import nel.bettershield.registry.BetterShieldCriteria;
import nel.bettershield.registry.BetterShieldEnchantments;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PlayerEntity.class)
public abstract class SlamImpactMixin {

    @Inject(method = "handleFallDamage", at = @At("HEAD"))
    public void onFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        if (player.getWorld().isClient) return;

        if (Bettershield.SLAM_START_Y.containsKey(player.getUuid())) {

            if (player.isTouchingWater()) {
                Bettershield.SLAM_START_Y.remove(player.getUuid());
                return;
            }

            double startY = Bettershield.SLAM_START_Y.get(player.getUuid());
            Bettershield.SLAM_START_Y.remove(player.getUuid());

            ItemStack stack = player.getStackInHand(Hand.MAIN_HAND);
            if (!(stack.getItem() instanceof ShieldItem)) {
                stack = player.getStackInHand(Hand.OFF_HAND);
            }

            var enchantmentRegistry = player.getWorld().getRegistryManager().getWrapperOrThrow(RegistryKeys.ENCHANTMENT);

            var foamEntry = enchantmentRegistry.getOptional(BetterShieldEnchantments.SLAM_FOAM);
            int levelFoam = foamEntry.isPresent() ? EnchantmentHelper.getLevel(foamEntry.get(), stack) : 0;
            if (levelFoam > 0) {
                float reduction = levelFoam * 0.25f;
                player.fallDistance = player.fallDistance * (1.0f - reduction);
            }

            ServerWorld world = (ServerWorld) player.getWorld();

            double cloudRadius = 3.0;
            int cloudCount = 20;

            for (int i = 0; i < cloudCount; i++) {
                double angle = (2 * Math.PI * i) / cloudCount;
                double x = player.getX() + Math.cos(angle) * cloudRadius;
                double z = player.getZ() + Math.sin(angle) * cloudRadius;
                double y = player.getY();

                float varSize = 0.1f + (world.random.nextFloat() * 0.4f);

                world.spawnParticles(
                        Bettershield.CLOUD_PARTICLE,
                        x, y, z,
                        1,
                        varSize,
                        varSize,
                        varSize,
                        0.05
                );
            }

            BlockPos hitPos = player.getBlockPos().down();
            BlockState state = world.getBlockState(hitPos);
            if (state.isAir() || state.getRenderType() == BlockRenderType.INVISIBLE) {
                state = Blocks.COBBLESTONE.getDefaultState();
            }

            Bettershield.SlamEffectPayload slamPayload = new Bettershield.SlamEffectPayload(
                    player.getX(), player.getY(), player.getZ(), Registries.BLOCK.getId(state.getBlock()).toString()
            );
            world.getPlayers().forEach(p -> ServerPlayNetworking.send((ServerPlayerEntity)p, slamPayload));

            var densityEntry = enchantmentRegistry.getOptional(BetterShieldEnchantments.SHIELD_DENSITY);
            int levelDensity = densityEntry.isPresent() ? EnchantmentHelper.getLevel(densityEntry.get(), stack) : 0;
            float damageMultiplierEnchant = 1.0f + (levelDensity * 0.10f);

            double distanceFallen = startY - player.getY();
            float impactDamage = 2.0f;
            if (distanceFallen > 3.0) impactDamage += (float) (distanceFallen - 3.0) * 1.0f;

            impactDamage = Math.min(impactDamage, 10.0f);
            impactDamage *= damageMultiplierEnchant;

            BetterShieldConfig config = Bettershield.getConfig();
            double radius = config.combat.slamRadius;
            Box box = player.getBoundingBox().expand(radius, 2.0, radius);

            List<Entity> targets = player.getWorld().getOtherEntities(player, box);
            int entitiesHit = 0;

            for (Entity target : targets) {
                if (target instanceof LivingEntity living) {
                    living.damage(player.getDamageSources().playerAttack(player), impactDamage);
                    living.addVelocity(0, 0.6, 0);
                    living.velocityModified = true;

                    if (config.stunMechanics.slamStunEnabled) {
                        var stunEntry = Registries.STATUS_EFFECT.getEntry(Bettershield.STUN_EFFECT);
                        living.addStatusEffect(new StatusEffectInstance(stunEntry, config.stunMechanics.stunDuration, 0, false, false, true));

                        Bettershield.StunMobsPayload stunPayload = new Bettershield.StunMobsPayload(living.getId(), config.stunMechanics.stunDuration);
                        world.getPlayers().forEach(p -> ServerPlayNetworking.send((ServerPlayerEntity)p, stunPayload));
                    }
                    entitiesHit++;
                }
            }

            if (entitiesHit >= 5 && player instanceof ServerPlayerEntity serverPlayer) {
                BetterShieldCriteria.grantAdvancement(serverPlayer, "john_cena");
            }

            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 1.0f, 0.5f);
            world.playSound(null, player.getBlockPos(), state.getSoundGroup().getBreakSound(), SoundCategory.PLAYERS, 2.0f, 0.8f);
        }
    }
}