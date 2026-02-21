package nel.bettershield.registry;

import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.item.Items;

public class BetterShieldLootModifier {
    public static void register() {
        // --- 1.20.5 FIX: The event only takes 3 parameters in this Fabric version! ---
        LootTableEvents.MODIFY.register((key, tableBuilder, source) -> {

            // Checking the raw string ID bypasses any Mojang field renaming issues
            String id = key.getValue().toString();

            if (id.equals("minecraft:chests/end_city_treasure")
                    || id.equals("minecraft:chests/stronghold_library")
                    || id.equals("minecraft:chests/stronghold_corridor")
                    || id.equals("minecraft:chests/ancient_city")) {

                LootPool.Builder poolBuilder = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1.0f))
                        .with(ItemEntry.builder(Items.BOOK).weight(5));

                tableBuilder.pool(poolBuilder);
            }
        });
    }
}