package growthcraft.core.shared.crafting.recipe;

import com.google.gson.JsonObject;
import growthcraft.bees.common.crafting.recipe.ShapelessRecipeWithBucket;
import growthcraft.core.shared.legacy.FluidContainerRegistry;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ShapelessRecipeContainerConversion extends ShapelessOreRecipe {

    public ShapelessRecipeContainerConversion(@Nullable ResourceLocation group, NonNullList<Ingredient> input, @Nonnull ItemStack result) {
        super(group, input, result);
    }

    @Nonnull
    @Override
    public String getGroup() {
        return group == null ? "" : group.toString();
    }

    /**
     * Add an empty bucket to the output of the recipe.
     *
     * @param inv Crafting inventory grid
     * @return List of ItemStacks to return
     */
    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.<ItemStack>withSize(inv.getSizeInventory(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getStackInSlot(i);
            nonnulllist.set(i, FluidContainerRegistry.getContainerItemWithFallback(itemstack));
        }

        return nonnulllist;
    }

    public static class Factory implements IRecipeFactory {
        @Override
        public IRecipe parse(JsonContext jsonContext, JsonObject jsonObject) {
            final String group = JsonUtils.getString(jsonObject, "group", "");
            final NonNullList<Ingredient> ingredients = UtilRecipeParser.parseShapeless(jsonContext, jsonObject);
            final ItemStack result = CraftingHelper.getItemStack(JsonUtils.getJsonObject(jsonObject, "result"), jsonContext);
            return new ShapelessRecipeWithBucket(group.isEmpty() ? null : new ResourceLocation(group), ingredients, result);
        }


    }

}
