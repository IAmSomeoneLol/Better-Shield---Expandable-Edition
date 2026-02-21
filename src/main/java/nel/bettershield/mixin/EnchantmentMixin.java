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

        // If the item in the anvil/table is a shield...
        if (stack.getItem() instanceof ShieldItem) {

            // ...and the enchantment is Loyalty or Piercing, force it to be allowed!
            if (self == Enchantments.LOYALTY || self == Enchantments.PIERCING) {
                cir.setReturnValue(true);
            }
        }
    }
}