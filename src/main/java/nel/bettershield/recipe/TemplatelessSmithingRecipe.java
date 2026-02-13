package nel.bettershield.recipe;

import com.google.gson.JsonObject;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.SmithingRecipe;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.world.World;

public class TemplatelessSmithingRecipe implements SmithingRecipe {
    private final Identifier id;
    private final Ingredient base;
    private final Ingredient addition;
    private final ItemStack result;

    public TemplatelessSmithingRecipe(Identifier id, Ingredient base, Ingredient addition, ItemStack result) {
        this.id = id;
        this.base = base;
        this.addition = addition;
        this.result = result;
    }

    @Override
    public boolean matches(Inventory inventory, World world) {
        // Slot 1 is Base, Slot 2 is Addition (Slot 0 is Template, we ignore/allow empty)
        return this.base.test(inventory.getStack(1)) && this.addition.test(inventory.getStack(2));
    }

    @Override
    public ItemStack craft(Inventory inventory, DynamicRegistryManager registryManager) {
        ItemStack itemStack = this.result.copy();
        ItemStack baseStack = inventory.getStack(1);

        // Copy tags (Enchantments, Banners, etc.)
        if (baseStack.hasNbt()) {
            itemStack.setNbt(baseStack.getNbt().copy());
        }
        return itemStack;
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
        return this.result;
    }

    @Override
    public boolean isIgnoredInRecipeBook() {
        return false;
    }

    @Override
    public Identifier getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.SMITHING;
    }

    @Override
    public boolean testTemplate(ItemStack stack) {
        // We return false here to indicate this recipe doesn't require a specific template item
        return false;
    }

    @Override
    public boolean testBase(ItemStack stack) {
        return this.base.test(stack);
    }

    @Override
    public boolean testAddition(ItemStack stack) {
        return this.addition.test(stack);
    }

    public static class Serializer implements RecipeSerializer<TemplatelessSmithingRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        // FIX 1: Implemented the JSON read method
        @Override
        public TemplatelessSmithingRecipe read(Identifier id, JsonObject json) {
            Ingredient base = Ingredient.fromJson(JsonHelper.getObject(json, "base"));
            Ingredient addition = Ingredient.fromJson(JsonHelper.getObject(json, "addition"));
            ItemStack result = ShapedRecipe.outputFromJson(JsonHelper.getObject(json, "result"));
            return new TemplatelessSmithingRecipe(id, base, addition, result);
        }

        @Override
        public TemplatelessSmithingRecipe read(Identifier id, PacketByteBuf buf) {
            Ingredient base = Ingredient.fromPacket(buf);
            Ingredient addition = Ingredient.fromPacket(buf);
            ItemStack result = buf.readItemStack();
            return new TemplatelessSmithingRecipe(id, base, addition, result);
        }

        @Override
        public void write(PacketByteBuf buf, TemplatelessSmithingRecipe recipe) {
            recipe.base.write(buf);
            recipe.addition.write(buf);
            buf.writeItemStack(recipe.result);
        }
    }
}