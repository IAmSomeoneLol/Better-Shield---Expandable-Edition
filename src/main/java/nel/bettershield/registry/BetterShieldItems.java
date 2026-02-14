package nel.bettershield.registry;

import nel.bettershield.Bettershield;
import nel.bettershield.item.ModShieldItem;
import nel.bettershield.recipe.BetterShieldDecorationRecipe;
// REMOVED: import nel.bettershield.recipe.TemplatelessSmithingRecipe;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class BetterShieldItems {

    // Diamond: Enchantability 10
    public static final Item DIAMOND_SHIELD = new ModShieldItem(
            new FabricItemSettings().maxCount(1),
            437,
            0.15f,
            0.10f,
            false,
            10
    );

    // Netherite: Enchantability 15
    public static final Item NETHERITE_SHIELD = new ModShieldItem(
            new FabricItemSettings().maxCount(1).fireproof(),
            538,
            0.25f,
            0.20f,
            true,
            15
    );

    public static final RecipeSerializer<BetterShieldDecorationRecipe> SHIELD_DECORATION_SERIALIZER = new SpecialRecipeSerializer<>(BetterShieldDecorationRecipe::new);

    public static void register() {
        Registry.register(Registries.ITEM, new Identifier(Bettershield.MOD_ID, "diamond_shield"), DIAMOND_SHIELD);
        Registry.register(Registries.ITEM, new Identifier(Bettershield.MOD_ID, "netherite_shield"), NETHERITE_SHIELD);

        // REMOVED: The custom "smithing_no_template" serializer registration

        Registry.register(Registries.RECIPE_SERIALIZER, new Identifier(Bettershield.MOD_ID, "shield_decoration"), SHIELD_DECORATION_SERIALIZER);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(content -> {
            content.addAfter(Items.SHIELD, DIAMOND_SHIELD);
            content.addAfter(DIAMOND_SHIELD, NETHERITE_SHIELD);
        });
    }
}