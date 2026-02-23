package nel.bettershield.item;

import net.minecraft.item.ShieldItem;

public class ModShieldItem extends ShieldItem {
    // Flag to tell the Mixin not to overwrite our custom enchantability
    public static final ThreadLocal<Boolean> IS_CUSTOM_BUILDING = ThreadLocal.withInitial(() -> false);

    private final float damageBonus;
    private final float cooldownReduction;
    private final boolean fireResistant;
    private final int enchantability;

    public ModShieldItem(Settings settings, int durability, float damageBonus, float cooldownReduction, boolean fireResistant, int enchantability) {
        super(settings.maxDamage(durability));
        this.damageBonus = damageBonus;
        this.cooldownReduction = cooldownReduction;
        this.fireResistant = fireResistant;
        this.enchantability = enchantability;
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
}