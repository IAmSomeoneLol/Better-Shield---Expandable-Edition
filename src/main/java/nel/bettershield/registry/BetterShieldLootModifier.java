package nel.bettershield.registry;

import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetEnchantmentsLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.item.Items;
import java.util.Map;

public class BetterShieldLootModifier {

    public static void register() {
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {

            // TARGET: End City & Stronghold Library & Ancient City & Stronghold Corridor
            if (LootTables.END_CITY_TREASURE_CHEST.equals(id)
                    || LootTables.STRONGHOLD_LIBRARY_CHEST.equals(id)
                    || LootTables.STRONGHOLD_CORRIDOR_CHEST.equals(id)
                    || LootTables.ANCIENT_CITY_CHEST.equals(id)) {

                LootPool.Builder poolBuilder = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1.0f))

                        // Existing Masterine
                        .with(ItemEntry.builder(Items.BOOK)
                                .weight(5)
                                .apply(new SetEnchantmentsLootFunction.Builder()
                                        .enchantment(BetterShieldEnchantments.MASTERINE, ConstantLootNumberProvider.create(1.0f))
                                )
                        )
                        // NEW: Active Armor (Always Level 1)
                        .with(ItemEntry.builder(Items.BOOK)
                                .weight(5) // Same rarity as Masterine
                                .apply(new SetEnchantmentsLootFunction.Builder()
                                        .enchantment(BetterShieldEnchantments.ACTIVE_ARMOR, ConstantLootNumberProvider.create(1.0f))
                                )
                        );

                tableBuilder.pool(poolBuilder);
            }
        });
    }
}