package nel.bettershield.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class EnchantmentMixin {

    @Inject(method = "isAcceptableItem", at = @At("HEAD"), cancellable = true)
    private void allowShieldEnchantments(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Enchantment self = (Enchantment) (Object) this;
        boolean isShield = stack.getItem() instanceof ShieldItem;

        // 1. Manage our Custom Mod Enchantments
        if (self == BetterShieldEnchantments.SHIELD_DENSITY ||
                self == BetterShieldEnchantments.PARRYFUL ||
                self == BetterShieldEnchantments.DEFLECTOR ||
                self == BetterShieldEnchantments.PARRY_DOCTRINE ||
                self == BetterShieldEnchantments.SLAM_FOAM ||
                self == BetterShieldEnchantments.MASTERINE ||
                self == BetterShieldEnchantments.ACTIVE_ARMOR) {

            // Force true if it's a shield. Force false if it's anything else (like a pickaxe).
            cir.setReturnValue(isShield);
            return;
        }

        // 2. Allow Vanilla Enchantments on Shields
        if (isShield) {
            if (self == Enchantments.LOYALTY || self == Enchantments.PIERCING) {
                cir.setReturnValue(true);
            }
        }
    }
}