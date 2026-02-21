package nel.bettershield.registry;

import nel.bettershield.Bettershield;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class BetterShieldEnchantments {

    // --- 1.20.5 FIX: Create basic programatic enchantments to prevent NullPointerExceptions ---
    public static final Enchantment SHIELD_DENSITY = new Enchantment(Enchantment.properties(
            Registries.ITEM.getOrCreateEntryList(net.minecraft.registry.tag.ItemTags.DURABILITY_ENCHANTABLE),
            10, 3, Enchantment.leveledCost(1, 10), Enchantment.leveledCost(1, 15), 1, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));

    public static final Enchantment PARRYFUL = new Enchantment(Enchantment.properties(
            Registries.ITEM.getOrCreateEntryList(net.minecraft.registry.tag.ItemTags.DURABILITY_ENCHANTABLE),
            10, 3, Enchantment.leveledCost(5, 8), Enchantment.leveledCost(5, 15), 1, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));

    public static final Enchantment DEFLECTOR = new Enchantment(Enchantment.properties(
            Registries.ITEM.getOrCreateEntryList(net.minecraft.registry.tag.ItemTags.DURABILITY_ENCHANTABLE),
            10, 2, Enchantment.leveledCost(10, 20), Enchantment.leveledCost(10, 30), 1, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));

    public static final Enchantment PARRY_DOCTRINE = new Enchantment(Enchantment.properties(
            Registries.ITEM.getOrCreateEntryList(net.minecraft.registry.tag.ItemTags.DURABILITY_ENCHANTABLE),
            10, 2, Enchantment.leveledCost(15, 20), Enchantment.leveledCost(15, 30), 1, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));

    public static final Enchantment SLAM_FOAM = new Enchantment(Enchantment.properties(
            Registries.ITEM.getOrCreateEntryList(net.minecraft.registry.tag.ItemTags.DURABILITY_ENCHANTABLE),
            10, 2, Enchantment.leveledCost(5, 15), Enchantment.leveledCost(5, 25), 1, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));

    public static final Enchantment MASTERINE = new Enchantment(Enchantment.properties(
            Registries.ITEM.getOrCreateEntryList(net.minecraft.registry.tag.ItemTags.DURABILITY_ENCHANTABLE),
            5, 2, Enchantment.leveledCost(5, 20), Enchantment.leveledCost(5, 30), 1, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));

    public static final Enchantment ACTIVE_ARMOR = new Enchantment(Enchantment.properties(
            Registries.ITEM.getOrCreateEntryList(net.minecraft.registry.tag.ItemTags.DURABILITY_ENCHANTABLE),
            1, 3, Enchantment.leveledCost(25, 50), Enchantment.leveledCost(25, 60), 1, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));


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