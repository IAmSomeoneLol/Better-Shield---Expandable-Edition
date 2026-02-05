package nel.bettershield.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.LoyaltyEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LoyaltyEnchantment.class)
public class LoyaltyEnchantmentMixin extends Enchantment {

    protected LoyaltyEnchantmentMixin(Rarity weight, EnchantmentTarget type, EquipmentSlot[] slotTypes) {
        super(weight, type, slotTypes);
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        // If it's a Shield, allow it! Otherwise, use default Trident logic.
        if (stack.getItem() instanceof ShieldItem) {
            return true;
        }
        return super.isAcceptableItem(stack);
    }
}