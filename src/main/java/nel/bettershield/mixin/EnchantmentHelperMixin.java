package nel.bettershield.mixin;

import nel.bettershield.registry.BetterShieldEnchantments;
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

    @Inject(method = "getPossibleEntries", at = @At("RETURN"), cancellable = true)
    private static void modifyPossibleEntries(FeatureSet enabledFeatures, int power, ItemStack stack, boolean treasureAllowed, CallbackInfoReturnable<List<EnchantmentLevelEntry>> cir) {
        List<EnchantmentLevelEntry> entries = cir.getReturnValue();
        if (entries == null) {
            entries = new ArrayList<>();
            cir.setReturnValue(entries);
        }

        boolean isShield = stack.getItem() instanceof ShieldItem;

        if (!isShield) {
            // Remove custom enchantments if the item in the table is NOT a shield
            entries.removeIf(entry ->
                    entry.enchantment == BetterShieldEnchantments.SHIELD_DENSITY ||
                            entry.enchantment == BetterShieldEnchantments.PARRYFUL ||
                            entry.enchantment == BetterShieldEnchantments.DEFLECTOR ||
                            entry.enchantment == BetterShieldEnchantments.PARRY_DOCTRINE ||
                            entry.enchantment == BetterShieldEnchantments.SLAM_FOAM ||
                            entry.enchantment == BetterShieldEnchantments.MASTERINE ||
                            entry.enchantment == BetterShieldEnchantments.ACTIVE_ARMOR
            );
        } else {
            // Add Vanilla Enchants to Shields (Passing in treasureAllowed!)
            addEntry(entries, power, Enchantments.LOYALTY, treasureAllowed);
            addEntry(entries, power, Enchantments.PIERCING, treasureAllowed);
            addEntry(entries, power, Enchantments.UNBREAKING, treasureAllowed);
            addEntry(entries, power, Enchantments.MENDING, treasureAllowed);

            // Add Custom Enchants to Shields
            addEntry(entries, power, BetterShieldEnchantments.SHIELD_DENSITY, treasureAllowed);
            addEntry(entries, power, BetterShieldEnchantments.PARRYFUL, treasureAllowed);
            addEntry(entries, power, BetterShieldEnchantments.DEFLECTOR, treasureAllowed);
            addEntry(entries, power, BetterShieldEnchantments.PARRY_DOCTRINE, treasureAllowed);
            addEntry(entries, power, BetterShieldEnchantments.SLAM_FOAM, treasureAllowed);
            addEntry(entries, power, BetterShieldEnchantments.MASTERINE, treasureAllowed);
            addEntry(entries, power, BetterShieldEnchantments.ACTIVE_ARMOR, treasureAllowed);
        }
    }

    // --- 1.20.5 FIX: Added Treasure check to block Mending from tables! ---
    private static void addEntry(List<EnchantmentLevelEntry> entries, int power, Enchantment enchantment, boolean treasureAllowed) {
        if (enchantment == null) return;

        // If it's a treasure enchant (like Mending) and we aren't allowed to give treasure, abort!
        if (enchantment.isTreasure() && !treasureAllowed) return;

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