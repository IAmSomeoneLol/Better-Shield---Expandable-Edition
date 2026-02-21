package nel.bettershield.recipe;

import nel.bettershield.registry.BetterShieldItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.item.BannerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.world.World;

public class BetterShieldDecorationRecipe extends SpecialCraftingRecipe {

    public BetterShieldDecorationRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    // --- 1.20.5 FIX: RecipeInputInventory changed to CraftingRecipeInput ---
    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        ItemStack shieldStack = ItemStack.EMPTY;
        ItemStack bannerStack = ItemStack.EMPTY;

        for (int i = 0; i < input.getSize(); ++i) {
            ItemStack stack = input.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof BannerItem) {
                if (!bannerStack.isEmpty()) return false;
                bannerStack = stack;
            } else if (isCustomShield(stack)) {
                if (!shieldStack.isEmpty()) return false;
                // --- 1.20.5 FIX: NBT check changed to Component check ---
                if (stack.contains(DataComponentTypes.BANNER_PATTERNS)) return false;
                shieldStack = stack;
            } else {
                return false;
            }
        }

        return !shieldStack.isEmpty() && !bannerStack.isEmpty();
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, DynamicRegistryManager registryManager) {
        ItemStack shieldStack = ItemStack.EMPTY;
        ItemStack bannerStack = ItemStack.EMPTY;

        for (int i = 0; i < input.getSize(); ++i) {
            ItemStack stack = input.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof BannerItem) {
                bannerStack = stack;
            } else if (isCustomShield(stack)) {
                shieldStack = stack.copy();
            }
        }

        if (shieldStack.isEmpty() || bannerStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // --- 1.20.5 FIX: Component Magic instead of NBT ---
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