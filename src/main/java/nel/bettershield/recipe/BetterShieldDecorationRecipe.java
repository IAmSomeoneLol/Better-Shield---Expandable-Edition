package nel.bettershield.recipe;

import nel.bettershield.registry.BetterShieldItems;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.BannerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.world.World;

public class BetterShieldDecorationRecipe extends SpecialCraftingRecipe {

    // --- 1.20.2 FIX: Removed 'Identifier id' from constructor ---
    public BetterShieldDecorationRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(RecipeInputInventory inventory, World world) {
        ItemStack shieldStack = ItemStack.EMPTY;
        ItemStack bannerStack = ItemStack.EMPTY;

        for (int i = 0; i < inventory.size(); ++i) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof BannerItem) {
                if (!bannerStack.isEmpty()) return false; // More than one banner
                bannerStack = stack;
            } else if (isCustomShield(stack)) {
                if (!shieldStack.isEmpty()) return false; // More than one shield
                if (stack.getSubNbt("BlockEntityTag") != null) return false; // Shield already has pattern
                shieldStack = stack;
            } else {
                return false; // Unknown item
            }
        }

        return !shieldStack.isEmpty() && !bannerStack.isEmpty();
    }

    @Override
    public ItemStack craft(RecipeInputInventory inventory, DynamicRegistryManager registryManager) {
        ItemStack shieldStack = ItemStack.EMPTY;
        ItemStack bannerStack = ItemStack.EMPTY;

        for (int i = 0; i < inventory.size(); ++i) {
            ItemStack stack = inventory.getStack(i);
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

        NbtCompound blockEntityTag = shieldStack.getOrCreateSubNbt("BlockEntityTag");
        NbtCompound bannerTag = bannerStack.getSubNbt("BlockEntityTag");

        // Base color
        blockEntityTag.putInt("Base", ((BannerItem)bannerStack.getItem()).getColor().getId());

        // Patterns
        if (bannerTag != null) {
            NbtList patterns = bannerTag.getList("Patterns", 10);
            blockEntityTag.put("Patterns", patterns.copy());
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