package nel.bettershield.mixin;

import nel.bettershield.registry.BetterShieldEnchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class ActiveArmorMixin {

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float reduceDamageWithActiveArmor(float amount, DamageSource source) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // 1. Check if holding item in OFFHAND specifically
        ItemStack offHand = entity.getOffHandStack();

        // 2. Check if it is a Shield (Vanilla or Modded)
        if (offHand.getItem() instanceof ShieldItem) {

            // 3. Check for Enchantment
            int level = EnchantmentHelper.getLevel(BetterShieldEnchantments.ACTIVE_ARMOR, offHand);

            if (level > 0) {
                // 4. Calculate Reduction
                // Level 1 = 4%, Level 2 = 8%, Level 3 = 12%
                float reduction = level * 0.04f;

                // Reduce damage
                return amount * (1.0f - reduction);
            }
        }

        return amount;
    }
}