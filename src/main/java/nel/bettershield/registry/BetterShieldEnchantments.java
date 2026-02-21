package nel.bettershield.registry;

import nel.bettershield.Bettershield;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

public class BetterShieldEnchantments {

    // --- 1.20.5 FIX: Drastically raised the "Max Power" ceiling so Netherite doesn't overflow! ---
    public static final Enchantment SHIELD_DENSITY = new Enchantment(Enchantment.properties(
            ItemTags.DURABILITY_ENCHANTABLE,
            10, 3, Enchantment.leveledCost(1, 11), Enchantment.leveledCost(51, 11), 1, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));

    public static final Enchantment PARRYFUL = new Enchantment(Enchantment.properties(
            ItemTags.DURABILITY_ENCHANTABLE,
            5, 3, Enchantment.leveledCost(5, 9), Enchantment.leveledCost(55, 9), 1, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));

    public static final Enchantment DEFLECTOR = new Enchantment(Enchantment.properties(
            ItemTags.DURABILITY_ENCHANTABLE,
            2, 2, Enchantment.leveledCost(10, 15), Enchantment.leveledCost(60, 15), 1, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));

    public static final Enchantment PARRY_DOCTRINE = new Enchantment(Enchantment.properties(
            ItemTags.DURABILITY_ENCHANTABLE,
            2, 2, Enchantment.leveledCost(12, 18), Enchantment.leveledCost(62, 18), 1, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));

    public static final Enchantment SLAM_FOAM = new Enchantment(Enchantment.properties(
            ItemTags.DURABILITY_ENCHANTABLE,
            5, 2, Enchantment.leveledCost(8, 14), Enchantment.leveledCost(58, 14), 1, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));

    public static final Enchantment MASTERINE = new Enchantment(Enchantment.properties(
            ItemTags.DURABILITY_ENCHANTABLE,
            2, 2, Enchantment.leveledCost(15, 15), Enchantment.leveledCost(65, 15), 1, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));

    public static final Enchantment ACTIVE_ARMOR = new Enchantment(Enchantment.properties(
            ItemTags.DURABILITY_ENCHANTABLE,
            1, 3, Enchantment.leveledCost(20, 12), Enchantment.leveledCost(70, 12), 1, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));


    public static void register() {
        Registry.register(Registries.ENCHANTMENT, new Identifier(Bettershield.MOD_ID, "shield_density"), SHIELD_DENSITY);
        Registry.register(Registries.ENCHANTMENT, new Identifier(Bettershield.MOD_ID, "parryful"), PARRYFUL);
        Registry.register(Registries.ENCHANTMENT, new Identifier(Bettershield.MOD_ID, "deflector"), DEFLECTOR);
        Registry.register(Registries.ENCHANTMENT, new Identifier(Bettershield.MOD_ID, "parry_doctrine"), PARRY_DOCTRINE);
        Registry.register(Registries.ENCHANTMENT, new Identifier(Bettershield.MOD_ID, "slam_foam"), SLAM_FOAM);
        Registry.register(Registries.ENCHANTMENT, new Identifier(Bettershield.MOD_ID, "masterine"), MASTERINE);
        Registry.register(Registries.ENCHANTMENT, new Identifier(Bettershield.MOD_ID, "active_armor"), ACTIVE_ARMOR);
    }
}