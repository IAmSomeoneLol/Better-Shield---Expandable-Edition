package nel.bettershield.registry;

import nel.bettershield.Bettershield;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class BetterShieldEnchantments {

    // ALL TARGETS SET TO 'BREAKABLE'
    public static final Enchantment SHIELD_DENSITY = new BasicShieldEnchantment(Enchantment.Rarity.COMMON, 3, false, 1, 10);
    public static final Enchantment PARRYFUL = new BasicShieldEnchantment(Enchantment.Rarity.COMMON, 3, false, 5, 8);
    public static final Enchantment DEFLECTOR = new BasicShieldEnchantment(Enchantment.Rarity.COMMON, 2, false, 10, 20);
    public static final Enchantment PARRY_DOCTRINE = new BasicShieldEnchantment(Enchantment.Rarity.COMMON, 2, false, 15, 20);
    public static final Enchantment SLAM_FOAM = new BasicShieldEnchantment(Enchantment.Rarity.COMMON, 2, false, 5, 15);
    public static final Enchantment MASTERINE = new BasicShieldEnchantment(Enchantment.Rarity.RARE, 2, false, 5, 20);

    public static void register() {
        register("shield_density", SHIELD_DENSITY);
        register("parryful", PARRYFUL);
        register("deflector", DEFLECTOR);
        register("parry_doctrine", PARRY_DOCTRINE);
        register("slam_foam", SLAM_FOAM);
        register("masterine", MASTERINE);
    }

    private static void register(String name, Enchantment enchantment) {
        Registry.register(Registries.ENCHANTMENT, new Identifier(Bettershield.MOD_ID, name), enchantment);
    }

    public static class BasicShieldEnchantment extends Enchantment {
        private final int maxLevel;
        private final boolean isTreasure;
        private final int minPowerBase;
        private final int powerPerLevel;

        public BasicShieldEnchantment(Rarity weight, int maxLevel, boolean isTreasure, int minPowerBase, int powerPerLevel) {

            super(weight, EnchantmentTarget.BREAKABLE, new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
            this.maxLevel = maxLevel;
            this.isTreasure = isTreasure;
            this.minPowerBase = minPowerBase;
            this.powerPerLevel = powerPerLevel;
        }

        @Override
        public int getMaxLevel() { return maxLevel; }

        @Override
        public boolean isTreasure() { return isTreasure; }

        @Override
        public int getMinPower(int level) { return this.minPowerBase + (level - 1) * this.powerPerLevel; }

        @Override
        public int getMaxPower(int level) { return super.getMinPower(level) + 50; }

        @Override
        public boolean isAcceptableItem(ItemStack stack) {
            return stack.getItem() instanceof ShieldItem;
        }
    }
}