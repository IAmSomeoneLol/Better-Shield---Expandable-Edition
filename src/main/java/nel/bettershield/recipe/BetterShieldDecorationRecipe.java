package nel.bettershield.recipe;

import nel.bettershield.registry.BetterShieldItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.BannerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

public class BetterShieldDecorationRecipe extends SpecialCraftingRecipe {

    public BetterShieldDecorationRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    public boolean matches(RecipeInputInventory input, World world) {
        ItemStack shieldStack = ItemStack.EMPTY;
        ItemStack bannerStack = ItemStack.EMPTY;

        // --- 1.20.5 FIX: getSize() changed to size() ---
        for (int i = 0; i < input.size(); ++i) {
            ItemStack stack = input.getStack(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof BannerItem) {
                if (!bannerStack.isEmpty()) return false;
                bannerStack = stack;
            } else if (isCustomShield(stack)) {
                if (!shieldStack.isEmpty()) return false;
                if (stack.contains(DataComponentTypes.BANNER_PATTERNS)) return false;
                shieldStack = stack;
            } else {
                return false;
            }
        }
        return !shieldStack.isEmpty() && !bannerStack.isEmpty();
    }

    public ItemStack craft(RecipeInputInventory input, RegistryWrapper.WrapperLookup lookup) {
        ItemStack shieldStack = ItemStack.EMPTY;
        ItemStack bannerStack = ItemStack.EMPTY;

        // --- 1.20.5 FIX: getSize() changed to size() ---
        for (int i = 0; i < input.size(); ++i) {
            ItemStack stack = input.getStack(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof BannerItem) {
                bannerStack = stack;
            } else if (isCustomShield(stack)) {
                shieldStack = stack.copy();
            }
        }

        if (shieldStack.isEmpty() || bannerStack.isEmpty()) return ItemStack.EMPTY;

        BannerItem bannerItem = (BannerItem) bannerStack.getItem();
        shieldStack.set(DataComponentTypes.BASE_COLOR, bannerItem.getColor());

        BannerPatternsComponent patterns = bannerStack.get(DataComponentTypes.BANNER_PATTERNS);
        if (patterns != null) {
            shieldStack.set(DataComponentTypes.BANNER_PATTERNS, patterns);
        }

        return shieldStack;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BetterShieldItems.SHIELD_DECORATION_SERIALIZER;
    }

    private boolean isCustomShield(ItemStack stack) {
        return stack.isOf(BetterShieldItems.DIAMOND_SHIELD) || stack.isOf(BetterShieldItems.NETHERITE_SHIELD);
    }
}