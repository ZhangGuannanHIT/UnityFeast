package cn.zgnhit.unityfeast.recipe;

import cn.zgnhit.unityfeast.UnityFeastMod;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

/** CraftingInput is trimmed by vanilla, so all three legal rows share this layout. */
public record SamePlanksBoardRecipe(ShapedRecipe shaped) implements CraftingRecipe {
    public static final MapCodec<SamePlanksBoardRecipe> CODEC=ShapedRecipe.MAP_CODEC.xmap(SamePlanksBoardRecipe::new,SamePlanksBoardRecipe::shaped);
    public static final StreamCodec<RegistryFriendlyByteBuf,SamePlanksBoardRecipe> STREAM_CODEC=ShapedRecipe.STREAM_CODEC.map(SamePlanksBoardRecipe::new,SamePlanksBoardRecipe::shaped);
    @Override public boolean matches(CraftingInput input,Level level){
        return input.width()==3&&input.height()==1&&shaped.matches(input,level)&&input.getItem(0).is(ItemTags.PLANKS)
                &&input.getItem(1).is(input.getItem(0).getItem())&&input.getItem(2).is(Items.STICK);
    }
    @Override public ItemStack assemble(CraftingInput i){return shaped.assemble(i);}
    @Override public RecipeSerializer<SamePlanksBoardRecipe> getSerializer(){return UnityFeastMod.BOARD_RECIPE.get();}
    @Override public CraftingBookCategory category(){return shaped.category();}
    @Override public String group(){return shaped.group();}
    @Override public boolean showNotification(){return shaped.showNotification();}
    @Override public PlacementInfo placementInfo(){return shaped.placementInfo();}
    @Override public List<RecipeDisplay> display(){return shaped.display();}
}
