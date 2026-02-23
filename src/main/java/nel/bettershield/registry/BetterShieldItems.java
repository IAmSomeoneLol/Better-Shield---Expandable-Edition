package nel.bettershield.registry;

import nel.bettershield.Bettershield;
import nel.bettershield.item.ModShieldItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.Component;
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
        ModShieldItem.IS_CUSTOM_BUILDING.set(true);

        Item.Settings diamondSettings = new Item.Settings().registryKey(DIAMOND_SHIELD_KEY);
        // 1.21.5 FIX: Copy the invisible blocking components from the Vanilla Shield!
        for (Component<?> component : Items.SHIELD.getComponents()) {
            copyComponent(diamondSettings, component);
        }
        diamondSettings.maxCount(1).enchantable(10).maxDamage(437);

        Item.Settings netheriteSettings = new Item.Settings().registryKey(NETHERITE_SHIELD_KEY).fireproof();
        // 1.21.5 FIX: Copy the invisible blocking components from the Vanilla Shield!
        for (Component<?> component : Items.SHIELD.getComponents()) {
            copyComponent(netheriteSettings, component);
        }
        netheriteSettings.maxCount(1).enchantable(15).maxDamage(538);

        DIAMOND_SHIELD = new ModShieldItem(diamondSettings, 437, 0.15f, 0.10f, false, 10);
        NETHERITE_SHIELD = new ModShieldItem(netheriteSettings, 538, 0.25f, 0.20f, true, 15);

        ModShieldItem.IS_CUSTOM_BUILDING.set(false);
    }

    @SuppressWarnings("unchecked")
    private static <T> void copyComponent(Item.Settings settings, Component<T> component) {
        settings.component(component.type(), component.value());
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