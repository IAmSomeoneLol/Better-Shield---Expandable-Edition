package nel.bettershield.registry;

import nel.bettershield.Bettershield;
import nel.bettershield.item.ModShieldItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class BetterShieldItems {

    public static final Item DIAMOND_SHIELD = new ModShieldItem(
            new Item.Settings().maxCount(1),
            437, 0.15f, 0.10f, false, 10
    );

    public static final Item NETHERITE_SHIELD = new ModShieldItem(
            new Item.Settings().maxCount(1).fireproof(),
            538, 0.25f, 0.20f, true, 15
    );

    public static void register() {
        Registry.register(Registries.ITEM, Identifier.of(Bettershield.MOD_ID, "diamond_shield"), DIAMOND_SHIELD);
        Registry.register(Registries.ITEM, Identifier.of(Bettershield.MOD_ID, "netherite_shield"), NETHERITE_SHIELD);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(content -> {
            content.addAfter(Items.SHIELD, DIAMOND_SHIELD);
            content.addAfter(DIAMOND_SHIELD, NETHERITE_SHIELD);
        });
    }
}