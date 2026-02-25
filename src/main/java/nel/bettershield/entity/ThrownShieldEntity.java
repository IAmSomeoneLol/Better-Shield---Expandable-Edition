package nel.bettershield.entity;

import nel.bettershield.BetterShieldConfig;
import nel.bettershield.Bettershield;
import nel.bettershield.item.ModShieldItem;
import nel.bettershield.registry.BetterShieldCriteria;
import nel.bettershield.registry.BetterShieldEnchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class ThrownShieldEntity extends PersistentProjectileEntity implements FlyingItemEntity {
    private static final TrackedData<ItemStack> SHIELD_STACK = DataTracker.registerData(ThrownShieldEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Boolean> IS_OFFHAND = DataTracker.registerData(ThrownShieldEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private boolean returning = false;
    private float impactDamage = 2.0f;
    private boolean stunEnabled = false;
    private int entitiesHitCount = 0;
    private final List<Integer> hitEntities = new ArrayList<>();

    public ThrownShieldEntity(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    public ThrownShieldEntity(World world, LivingEntity owner, ItemStack stack) {
        super(Bettershield.THROWN_SHIELD_ENTITY_TYPE, owner.getX(), owner.getEyeY() - 0.10000000149011612D, owner.getZ(), world, new ItemStack(Items.SHIELD), new ItemStack(Items.SHIELD));
        this.setOwner(owner);
        this.setStack(stack);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(SHIELD_STACK, new ItemStack(Items.SHIELD));
        builder.add(IS_OFFHAND, false);
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(Items.SHIELD);
    }

    public void setStack(ItemStack stack) {
        this.dataTracker.set(SHIELD_STACK, stack);
    }

    @Override
    public ItemStack getStack() {
        return this.dataTracker.get(SHIELD_STACK);
    }

    public void setOriginalSlot(boolean isOffhand) {
        this.dataTracker.set(IS_OFFHAND, isOffhand);
    }

    public void setImpactDamage(float damage) {
        this.impactDamage = damage;
    }

    public void setStunEnabled(boolean enabled) {
        this.stunEnabled = enabled;
    }

    @Override
    public void tick() {
        if (this.inGroundTime > 4) {
            this.returning = true;
        }

        Entity owner = this.getOwner();
        BetterShieldConfig config = Bettershield.getConfig();
        double maxRange = config.combat.shieldThrowRange;

        if (owner != null && !this.returning) {
            if (this.distanceTo(owner) > maxRange) {
                this.returning = true;
            }
        }

        if ((this.returning || this.isNoClip()) && owner != null) {
            this.setNoClip(true);
            this.pickupType = PickupPermission.DISALLOWED;

            Vec3d ownerPos = owner.getEyePos().subtract(0, 0.5, 0);
            Vec3d toOwner = ownerPos.subtract(new Vec3d(this.getX(), this.getY(), this.getZ()));
            double distance = toOwner.length();

            this.setPos(this.getX(), this.getY() + toOwner.y * 0.015, this.getZ());
            if (this.getEntityWorld().isClient()) this.lastRenderY = this.getY();

            double speed = 0.5;
            double drag = 0.95;

            if (distance < 5.0) {
                drag = 0.80;
            }

            this.setVelocity(this.getVelocity().multiply(drag).add(toOwner.normalize().multiply(speed)));

            if (distance < 2.0D && !owner.isSpectator()) {
                tryCatchShield(owner);
            }
        }

        super.tick();
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        if (this.age < 5) return;
        if (this.isOwner(player) || this.getOwner() == null) {
            if (!this.returning && !this.getEntityWorld().isClient()) {
                tryCatchShield(player);
            }
        }
    }

    private void tryCatchShield(Entity owner) {
        if (!this.getEntityWorld().isClient() && owner instanceof PlayerEntity player) {
            if (player.isCreative()) {
                this.discard();
                player.playSound(SoundEvents.ITEM_TRIDENT_RETURN, 1.0F, 1.0F);
                return;
            }

            ItemStack shield = this.getStack();
            boolean wasOffhand = this.dataTracker.get(IS_OFFHAND);

            if (wasOffhand) {
                ItemStack currentItem = player.getOffHandStack();
                if (currentItem.isEmpty()) {
                    player.setStackInHand(Hand.OFF_HAND, shield);
                } else {
                    if (player.getInventory().insertStack(currentItem)) {
                        player.setStackInHand(Hand.OFF_HAND, shield);
                    } else {
                        player.dropItem(currentItem, false);
                        player.setStackInHand(Hand.OFF_HAND, shield);
                    }
                }
            } else {
                if (player.getMainHandStack().isEmpty()) {
                    player.setStackInHand(Hand.MAIN_HAND, shield);
                } else {
                    if (!player.getInventory().insertStack(shield)) {
                        player.dropItem(shield, false);
                    }
                }
            }

            this.discard();
            player.playSound(SoundEvents.ITEM_TRIDENT_RETURN, 1.0F, 1.0F);
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        HitResult.Type type = hitResult.getType();
        if (type == HitResult.Type.ENTITY) {
            this.onEntityHit((EntityHitResult) hitResult);
        } else if (type == HitResult.Type.BLOCK) {
            this.returning = true;
            this.setVelocity(this.getVelocity().multiply(-0.1));
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.age < 3 && entityHitResult.getEntity() == this.getOwner()) return;
        if (this.returning) return;

        Entity target = entityHitResult.getEntity();

        if (this.hitEntities.contains(target.getId())) return;
        this.hitEntities.add(target.getId());

        int piercingLevel = 0;
        var enchantmentRegistry = this.getEntityWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        var piercingEntry = enchantmentRegistry.getOptional(Enchantments.PIERCING);
        if (piercingEntry.isPresent()) {
            piercingLevel = EnchantmentHelper.getLevel(piercingEntry.get(), this.getStack());
        }

        this.entitiesHitCount++;

        if (this.entitiesHitCount > piercingLevel) {
            this.returning = true;
            this.setNoClip(true);
            this.setVelocity(this.getVelocity().multiply(-0.1));
        } else {
            this.setVelocity(this.getVelocity().multiply(0.95));
            if (!this.getEntityWorld().isClient()) {
                this.getEntityWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_TRIDENT_HIT, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 2.0f);
            }
        }

        if (this.getEntityWorld().isClient()) return;

        float damage = this.impactDamage;
        float damageMult = 1.0f;

        var densityEntry = enchantmentRegistry.getOptional(BetterShieldEnchantments.SHIELD_DENSITY);
        if (densityEntry.isPresent()) {
            int density = EnchantmentHelper.getLevel(densityEntry.get(), this.getStack());
            damageMult += (density * 0.1f);
        }

        if (this.getStack().getItem() instanceof ModShieldItem modShield) {
            damageMult += modShield.getDamageBonus();
        }

        damage *= damageMult;

        Entity owner = this.getOwner();
        if (owner instanceof LivingEntity livingOwner) {
            target.damage((ServerWorld) this.getEntityWorld(), this.getDamageSources().trident(this, livingOwner), damage);

            if (this.stunEnabled && target instanceof LivingEntity livingTarget) {
                BetterShieldConfig config = Bettershield.getConfig();
                var stunEntry = Registries.STATUS_EFFECT.getEntry(Bettershield.STUN_EFFECT);

                if (stunEntry != null) {
                    livingTarget.addStatusEffect(new StatusEffectInstance(stunEntry, config.stunMechanics.stunDuration, 0, false, false, true));
                    Bettershield.StunMobsPayload stunPayload = new Bettershield.StunMobsPayload(livingTarget.getId(), config.stunMechanics.stunDuration);
                    ((ServerWorld) this.getEntityWorld()).getPlayers().forEach(p -> net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send((ServerPlayerEntity)p, stunPayload));
                }
            }

            if (owner instanceof ServerPlayerEntity serverPlayer) {
                var loyaltyEntry = enchantmentRegistry.getOptional(Enchantments.LOYALTY);
                if (loyaltyEntry.isPresent() && EnchantmentHelper.getLevel(loyaltyEntry.get(), this.getStack()) > 0) {
                    BetterShieldCriteria.grantAdvancement(serverPlayer, "throw_joke");
                }

                if (this.entitiesHitCount >= 3) {
                    BetterShieldCriteria.grantAdvancement(serverPlayer, "captain_america");
                }
            }

            ItemStack stack = this.getStack();
            if (stack.isDamageable()) {
                stack.setDamage(stack.getDamage() + 2);
                if (stack.getDamage() >= stack.getMaxDamage()) {
                    this.discard();
                    this.getEntityWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_SHIELD_BREAK, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
                    return;
                }
                this.setStack(stack);
            }

        } else {
            target.damage((ServerWorld) this.getEntityWorld(), this.getDamageSources().thrown(this, this.getOwner()), damage);
        }
    }

    @Override
    protected boolean canHit(Entity entity) {
        if (this.hitEntities.contains(entity.getId())) {
            return false;
        }
        return super.canHit(entity);
    }
}