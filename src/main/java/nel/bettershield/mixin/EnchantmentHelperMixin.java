package nel.bettershield.mixin;

import nel.bettershield.registry.BetterShieldEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @Inject(method = "getPossibleEntries", at = @At("RETURN"), cancellable = true)
    private static void modifyPossibleEntries(int power, ItemStack stack, boolean treasureAllowed, CallbackInfoReturnable<List<EnchantmentLevelEntry>> cir) {
        List<EnchantmentLevelEntry> entries = cir.getReturnValue();

        // 1. CLEANUP: Remove our Shield Enchants from Non-Shields (Armor, Tools)
        if (!stack.isOf(Items.SHIELD)) {
            entries.removeIf(entry -> entry.enchantment instanceof BetterShieldEnchantments.BasicShieldEnchantment);
            return;
        }

        // 2. ADDITIONS: If it IS a Shield, add the missing Vanilla enchants
        if (stack.isOf(Items.SHIELD)) {
            addEntry(entries, power, Enchantments.LOYALTY);
            addEntry(entries, power, Enchantments.PIERCING);
            addEntry(entries, power, Enchantments.UNBREAKING);
            addEntry(entries, power, Enchantments.MENDING);
        }
    }

    private static void addEntry(List<EnchantmentLevelEntry> entries, int power, Enchantment enchantment) {
        // Don't add if already present
        for (EnchantmentLevelEntry entry : entries) {
            if (entry.enchantment == enchantment) return;
        }

        // Check if power matches
        for (int level = enchantment.getMaxLevel(); level >= enchantment.getMinLevel(); level--) {
            if (enchantment.getMinPower(level) <= power && enchantment.getMaxPower(level) >= power) {
                entries.add(new EnchantmentLevelEntry(enchantment, level));
                break;
            }
        }
    }
}