package nel.bettershield.mixin;

import nel.bettershield.BetterShieldConfig;
import nel.bettershield.Bettershield;
import nel.bettershield.registry.BetterShieldCriteria;
import nel.bettershield.registry.BetterShieldEnchantments;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
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
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
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

            int levelFoam = EnchantmentHelper.getLevel(BetterShieldEnchantments.SLAM_FOAM, stack);
            if (levelFoam > 0) {
                float reduction = levelFoam * 0.25f;
                player.fallDistance = player.fallDistance * (1.0f - reduction);
            }

            ServerWorld world = (ServerWorld) player.getWorld();

            // --- VISUALS ---
            BlockPos hitPos = player.getBlockPos().down();
            BlockState state = world.getBlockState(hitPos);
            if (state.isAir() || state.getRenderType() == BlockRenderType.INVISIBLE) {
                state = Blocks.COBBLESTONE.getDefaultState();
            }

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeDouble(player.getX());
            buf.writeDouble(player.getY());
            buf.writeDouble(player.getZ());
            buf.writeString(Registries.BLOCK.getId(state.getBlock()).toString());
            world.getChunkManager().sendToNearbyPlayers(player, ServerPlayNetworking.createS2CPacket(Bettershield.PACKET_SLAM_EFFECT, buf));

            // --- DAMAGE CALCULATION ---
            int levelDensity = EnchantmentHelper.getLevel(BetterShieldEnchantments.SHIELD_DENSITY, stack);
            float damageMultiplierEnchant = 1.0f + (levelDensity * 0.10f);

            double distanceFallen = startY - player.getY();
            float impactDamage = 2.0f;
            if (distanceFallen > 3.0) impactDamage += (float) (distanceFallen - 3.0) * 1.0f;

            impactDamage = Math.min(impactDamage, 10.0f);
            impactDamage *= damageMultiplierEnchant;

            // --- AREA OF EFFECT ---
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

                    // UPDATED: Use StunMechanics category
                    if (config.stunMechanics.slamStunEnabled) {
                        living.addStatusEffect(new StatusEffectInstance(Bettershield.STUN_EFFECT, config.stunMechanics.stunDuration, 0, false, false, true));

                        PacketByteBuf stunBuf = PacketByteBufs.create();
                        stunBuf.writeInt(living.getId());
                        stunBuf.writeInt(config.stunMechanics.stunDuration);
                        world.getChunkManager().sendToNearbyPlayers(living, ServerPlayNetworking.createS2CPacket(Bettershield.PACKET_STUN_MOBS, stunBuf));
                    }

                    entitiesHit++;
                }
            }

            if (entitiesHit >= 5 && player instanceof ServerPlayerEntity serverPlayer) {
                BetterShieldCriteria.SLAM_MULTI.trigger(serverPlayer);
            }

            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 1.0f, 0.5f);
            world.playSound(null, player.getBlockPos(), state.getSoundGroup().getBreakSound(), SoundCategory.PLAYERS, 2.0f, 0.8f);
        }
    }
}