package nel.bettershield.item;

import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;

public class ModShieldItem extends ShieldItem {
    private final float damageBonus;
    private final float cooldownReduction;
    private final boolean fireResistant;
    private final int enchantability; // NEW FIELD

    public ModShieldItem(Settings settings, int durability, float damageBonus, float cooldownReduction, boolean fireResistant, int enchantability) {
        super(settings.maxDamage(durability));
        this.damageBonus = damageBonus;
        this.cooldownReduction = cooldownReduction;
        this.fireResistant = fireResistant;
        this.enchantability = enchantability;
    }

    // This method tells the Enchanting Table how "magical" the item is.
    // 0 = Not enchantable in table (Vanilla Shield)
    // 10 = Diamond Tier
    // 15 = Netherite/Gold Tier
    @Override
    public int getEnchantability() {
        return this.enchantability;
    }

    public float getDamageBonus() {
        return damageBonus;
    }

    public float getCooldownReduction() {
        return cooldownReduction;
    }

    public boolean isFireResistant() {
        return fireResistant;
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return super.canRepair(stack, ingredient);
    }
}