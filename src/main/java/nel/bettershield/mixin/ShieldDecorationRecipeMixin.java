package nel.bettershield.mixin;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.recipe.ShieldDecorationRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ShieldDecorationRecipe.class)
public class ShieldDecorationRecipeMixin {

    // Vanilla logic asks: "Is this item specifically the Vanilla Shield?"
    // We interrupt that question and answer: "Is this item ANY kind of Shield?"
    @Redirect(method = "matches", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z"))
    private boolean allowCustomShields(ItemStack instance, Item item) {
        // If the game is checking for the vanilla shield item...
        if (item == Items.SHIELD) {
            // Return TRUE if the item in the slot is an instance of ShieldItem (which includes Diamond/Netherite)
            return instance.getItem() instanceof ShieldItem;
        }
        // Otherwise, behave normally (this preserves checks for the Banner item)
        return instance.isOf(item);
    }
}