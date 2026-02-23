package nel.bettershield.registry;

import nel.bettershield.Bettershield;
import nel.bettershield.item.ModShieldItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class BetterShieldItems {

    public static final RegistryKey<Item> DIAMOND_SHIELD_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Bettershield.MOD_ID, "diamond_shield"));
    public static final RegistryKey<Item> NETHERITE_SHIELD_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Bettershield.MOD_ID, "netherite_shield"));

    public static final Item DIAMOND_SHIELD;
    public static final Item NETHERITE_SHIELD;

    static {
        // Turn on the flag so the Mixin knows we are building our custom shields
        ModShieldItem.IS_CUSTOM_BUILDING.set(true);

        // 1.21.2 FIX: Use .enchantable() explicitly so the Enchanting Table accepts them
        DIAMOND_SHIELD = new ModShieldItem(
                new Item.Settings().registryKey(DIAMOND_SHIELD_KEY).maxCount(1).enchantable(10),
                437, 0.15f, 0.10f, false, 10
        );

        NETHERITE_SHIELD = new ModShieldItem(
                new Item.Settings().registryKey(NETHERITE_SHIELD_KEY).maxCount(1).fireproof().enchantable(15),
                538, 0.25f, 0.20f, true, 15
        );

        // Turn the flag off
        ModShieldItem.IS_CUSTOM_BUILDING.set(false);
    }

    public static void register() {
        Registry.register(Registries.ITEM, DIAMOND_SHIELD_KEY, DIAMOND_SHIELD);
        Registry.register(Registries.ITEM, NETHERITE_SHIELD_KEY, NETHERITE_SHIELD);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(content -> {
            content.addAfter(Items.SHIELD, DIAMOND_SHIELD);
            content.addAfter(DIAMOND_SHIELD, NETHERITE_SHIELD);
        });
    }
}