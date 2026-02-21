package nel.bettershield.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.resource.featuretoggle.FeatureSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    // --- 1.20.5 FIX: Added FeatureSet to the exact method signature! ---
    @Inject(method = "getPossibleEntries", at = @At("RETURN"), cancellable = true)
    private static void modifyPossibleEntries(FeatureSet enabledFeatures, int power, ItemStack stack, boolean treasureAllowed, CallbackInfoReturnable<List<EnchantmentLevelEntry>> cir) {
        List<EnchantmentLevelEntry> entries = cir.getReturnValue();
        if (entries == null) {
            entries = new ArrayList<>();
            cir.setReturnValue(entries);
        }

        boolean isAnyShield = stack.getItem() instanceof ShieldItem;

        if (isAnyShield) {
            addEntry(entries, power, Enchantments.LOYALTY);
            addEntry(entries, power, Enchantments.PIERCING);
            addEntry(entries, power, Enchantments.UNBREAKING);
            addEntry(entries, power, Enchantments.MENDING);
        }
    }

    private static void addEntry(List<EnchantmentLevelEntry> entries, int power, Enchantment enchantment) {
        for (EnchantmentLevelEntry entry : entries) {
            if (entry.enchantment == enchantment) return;
        }
        for (int level = enchantment.getMaxLevel(); level >= enchantment.getMinLevel(); level--) {
            if (enchantment.getMinPower(level) <= power && enchantment.getMaxPower(level) >= power) {
                entries.add(new EnchantmentLevelEntry(enchantment, level));
                break;
            }
        }
    }
}