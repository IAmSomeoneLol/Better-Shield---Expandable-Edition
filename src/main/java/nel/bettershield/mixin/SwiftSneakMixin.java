package nel.bettershield.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class SwiftSneakMixin {

    @Shadow public abstract boolean isBlocking();
    @Shadow public abstract ItemStack getActiveItem();

    // Intercept the movement input vector (WASD) entering the travel() method
    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3d modifyMovementInput(Vec3d input) {
        // Cast 'this' safely
        LivingEntity self = (LivingEntity) (Object) this;

        // 1. Only run for Players
        if (!(self instanceof PlayerEntity player)) {
            return input;
        }

        // 2. Logic: Are we holding a Shield and Blocking?
        if (this.isBlocking() && this.getActiveItem().isOf(Items.SHIELD)) {

            // 3. Logic: Check for Swift Sneak on Leggings
            int level = EnchantmentHelper.getEquipmentLevel(Enchantments.SWIFT_SNEAK, player);

            if (level > 0) {
                // Vanilla Blocking Speed = 0.2 (20%)
                // New Bonus: +0.18 (18%) per level.

                double baseSpeed = 0.2;
                double addedSpeed = level * 0.18;
                double targetSpeed = baseSpeed + addedSpeed;

                // --- CALCULATION ---
                // Level 1: 0.20 + 0.18 = 0.38 (38% speed)
                // Level 2: 0.20 + 0.36 = 0.56 (56% speed)
                // Level 3: 0.20 + 0.54 = 0.74 (74% speed)

                // Calculate the multiplier needed to scale the vector up
                double multiplier = targetSpeed / baseSpeed;

                // Scale X and Z (Horizontal movement), keep Y (Gravity) same
                return input.multiply(multiplier, 1.0, multiplier);
            }
        }

        return input;
    }
}