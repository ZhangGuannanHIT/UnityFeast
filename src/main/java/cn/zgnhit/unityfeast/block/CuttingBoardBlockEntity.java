package cn.zgnhit.unityfeast.block;

import cn.zgnhit.unityfeast.UnityFeastMod;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/** A passive, single authoritative ingredient slot; never ticks. */
public final class CuttingBoardBlockEntity extends BlockEntity {
    private ItemStack ingredient=ItemStack.EMPTY;
    public CuttingBoardBlockEntity(BlockPos p,BlockState s){super(UnityFeastMod.CUTTING_BOARD_ENTITY.get(),p,s);}
    public static boolean accepts(ItemStack s){return s.is(Items.PORKCHOP)||s.is(Items.SALMON)||s.is(Items.CHICKEN);}
    public ItemStack ingredient(){return ingredient.copy();}
    public void setIngredient(ItemStack stack){
        ingredient=accepts(stack)?stack.copyWithCount(1):ItemStack.EMPTY;
        setChanged();
        if(level!=null&&!level.isClientSide())level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    public ItemStack removeIngredient(){var old=ingredient();setIngredient(ItemStack.EMPTY);return old;}
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(!ingredient.isEmpty())out.store("ingredient",ItemStack.CODEC,ingredient);}
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);var s=in.read("ingredient",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        ingredient=accepts(s)?s.copyWithCount(1):ItemStack.EMPTY;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveCustomOnly(r);}
}
