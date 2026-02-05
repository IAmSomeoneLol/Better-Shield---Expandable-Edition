package nel.bettershield.mixin;

import net.minecraft.item.Item;
import net.minecraft.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ShieldItem.class)
public abstract class ShieldItemMixin extends Item {

    public ShieldItemMixin(Settings settings) {
        super(settings);
    }

    @Override
    public int getEnchantability() {
        // Vanilla is 0. Iron is 14, Diamond is 10.
        // Let's give Shields "10" so they are decent to enchant.
        return 10;
    }

    @Override
    public boolean isEnchantable(net.minecraft.item.ItemStack stack) {
        return true;
    }
}