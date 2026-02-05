package nel.bettershield.entity;

import nel.bettershield.BetterShieldConfig;
import nel.bettershield.Bettershield;
import nel.bettershield.registry.BetterShieldCriteria; // NEW IMPORT
import nel.bettershield.registry.BetterShieldEnchantments;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ThrownShieldEntity extends PersistentProjectileEntity {
    private static final TrackedData<ItemStack> SHIELD_STACK = DataTracker.registerData(ThrownShieldEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Boolean> IS_OFFHAND = DataTracker.registerData(ThrownShieldEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private boolean returning = false;
    private float impactDamage = 2.0f;
    private boolean stunEnabled = false;
    private int entitiesHitCount = 0;

    public ThrownShieldEntity(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    public ThrownShieldEntity(World world, LivingEntity owner, ItemStack stack) {
        super(Bettershield.THROWN_SHIELD_ENTITY_TYPE, owner, world);
        this.setStack(stack);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SHIELD_STACK, new ItemStack(Items.SHIELD));
        this.dataTracker.startTracking(IS_OFFHAND, false);
    }

    public void setStack(ItemStack stack) {
        this.dataTracker.set(SHIELD_STACK, stack);
    }

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
            Vec3d toOwner = ownerPos.subtract(this.getPos());
            double distance = toOwner.length();

            this.setPos(this.getX(), this.getY() + toOwner.y * 0.015, this.getZ());
            if (this.getWorld().isClient) this.lastRenderY = this.getY();

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
            if (!this.returning && !this.getWorld().isClient) {
                tryCatchShield(player);
            }
        }
    }

    private void tryCatchShield(Entity owner) {
        if (!this.getWorld().isClient && owner instanceof PlayerEntity player) {
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
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.age < 3 && entityHitResult.getEntity() == this.getOwner()) return;
        if (this.returning) return;

        Entity target = entityHitResult.getEntity();
        float damage = this.impactDamage;

        int density = EnchantmentHelper.getLevel(BetterShieldEnchantments.SHIELD_DENSITY, this.getStack());
        damage += (damage * (density * 0.1f));

        Entity owner = this.getOwner();
        if (owner instanceof LivingEntity livingOwner) {
            target.damage(this.getDamageSources().trident(this, livingOwner), damage);

            // STUN LOGIC
            if (this.stunEnabled && target instanceof LivingEntity livingTarget) {
                BetterShieldConfig config = Bettershield.getConfig();
                livingTarget.addStatusEffect(new StatusEffectInstance(Bettershield.STUN_EFFECT, config.stunMechanics.stunDuration, 0, false, false, true));

                PacketByteBuf stunBuf = PacketByteBufs.create();
                stunBuf.writeInt(livingTarget.getId());
                stunBuf.writeInt(config.stunMechanics.stunDuration);
                if (this.getWorld().isClient == false) {
                    this.getWorld().getPlayers().forEach(p -> ServerPlayNetworking.send((ServerPlayerEntity)p, Bettershield.PACKET_STUN_MOBS, stunBuf));
                }
            }

            // --- ADVANCEMENT TRIGGERS ---
            if (owner instanceof ServerPlayerEntity serverPlayer) {
                // 1. A Throwaway Captain Joke (Loyalty + Hit)
                if (EnchantmentHelper.getLevel(Enchantments.LOYALTY, this.getStack()) > 0) {
                    BetterShieldCriteria.SHIELD_THROW_HIT.trigger(serverPlayer);
                }

                // 2. CAPTAIN AMERICA (3rd Hit in one throw)
                // We increment later, so currently hitting index 'entitiesHitCount'.
                // If entitiesHitCount is 2, this is the 3rd hit (0, 1, 2).
                if (this.entitiesHitCount == 2) {
                    BetterShieldCriteria.CAPTAIN_AMERICA.trigger(serverPlayer);
                }
            }

            // DURABILITY
            ItemStack stack = this.getStack();
            if (stack.isDamageable()) {
                stack.setDamage(stack.getDamage() + 2);
                if (stack.getDamage() >= stack.getMaxDamage()) {
                    this.discard();
                    this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_SHIELD_BREAK, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
                    return;
                }
                this.setStack(stack);
            }

        } else {
            target.damage(this.getDamageSources().thrown(this, this.getOwner()), damage);
        }

        // PIERCING LOGIC
        int piercingLevel = EnchantmentHelper.getLevel(Enchantments.PIERCING, this.getStack());
        this.entitiesHitCount++;

        if (this.entitiesHitCount > piercingLevel) {
            this.returning = true;
            this.setVelocity(this.getVelocity().multiply(-0.1));
        } else {
            this.setVelocity(this.getVelocity().multiply(0.95));
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_TRIDENT_HIT, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 2.0f);
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            this.returning = true;
        }
    }

    @Override
    protected ItemStack asItemStack() {
        return this.getStack();
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.put("Shield", this.getStack().writeNbt(new NbtCompound()));
        nbt.putBoolean("Returning", this.returning);
        nbt.putBoolean("IsOffhand", this.dataTracker.get(IS_OFFHAND));
        nbt.putFloat("ImpactDamage", this.impactDamage);
        nbt.putBoolean("StunEnabled", this.stunEnabled);
        nbt.putInt("EntitiesHit", this.entitiesHitCount);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("Shield", 10)) {
            this.setStack(ItemStack.fromNbt(nbt.getCompound("Shield")));
        }
        this.returning = nbt.getBoolean("Returning");
        this.dataTracker.set(IS_OFFHAND, nbt.getBoolean("IsOffhand"));
        if (nbt.contains("ImpactDamage")) {
            this.impactDamage = nbt.getFloat("ImpactDamage");
        }
        this.stunEnabled = nbt.getBoolean("StunEnabled");
        this.entitiesHitCount = nbt.getInt("EntitiesHit");
    }
}