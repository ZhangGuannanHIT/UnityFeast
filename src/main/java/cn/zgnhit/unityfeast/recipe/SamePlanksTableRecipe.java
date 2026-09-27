package cn.zgnhit.unityfeast.recipe;

import cn.zgnhit.unityfeast.UnityFeastMod;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

/** Delegate vanilla layout, displays and wire format; add item-identity validation. */
public final class SamePlanksTableRecipe implements CraftingRecipe {
    public static final MapCodec<SamePlanksTableRecipe> CODEC = ShapedRecipe.MAP_CODEC.xmap(SamePlanksTableRecipe::new, r -> r.shaped);
    public static final StreamCodec<RegistryFriendlyByteBuf, SamePlanksTableRecipe> STREAM_CODEC = ShapedRecipe.STREAM_CODEC.map(SamePlanksTableRecipe::new, r -> r.shaped);
    private final ShapedRecipe shaped;
    public SamePlanksTableRecipe(ShapedRecipe shaped) { this.shaped = shaped; }
    @Override public boolean matches(CraftingInput input, Level level) {
        if (!shaped.matches(input, level) || input.width() != 3 || input.height() != 3) return false;
        ItemStack first = input.getItem(0);
        return first.is(ItemTags.PLANKS) && input.getItem(1).is(first.getItem()) && input.getItem(2).is(first.getItem());
    }
    @Override public ItemStack assemble(CraftingInput input) { return shaped.assemble(input); }
    @Override public RecipeSerializer<SamePlanksTableRecipe> getSerializer() { return UnityFeastMod.TABLE_RECIPE.get(); }
    @Override public CraftingBookCategory category() { return shaped.category(); }
    @Override public String group() { return shaped.group(); }
    @Override public boolean showNotification() { return shaped.showNotification(); }
    @Override public PlacementInfo placementInfo() { return shaped.placementInfo(); }
    @Override public List<RecipeDisplay> display() { return shaped.display(); }
}
