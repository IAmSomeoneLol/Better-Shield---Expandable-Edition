package nel.bettershield.mixin;

import nel.bettershield.item.ModShieldItem;
import net.minecraft.item.Item;
import net.minecraft.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ShieldItem.class)
public abstract class VanillaShieldEnchantableMixin {

    // 1.21.2 FIX: Appends the enchantable component to the Vanilla Shield.
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static Item.Settings makeVanillaShieldEnchantable(Item.Settings settings) {
        // If we are currently building a Custom Shield, ignore this Mixin.
        if (ModShieldItem.IS_CUSTOM_BUILDING.get()) {
            return settings;
        }
        // Otherwise, it's the Vanilla Shield! Give it an enchantability level of 14.
        return settings.enchantable(14);
    }
}