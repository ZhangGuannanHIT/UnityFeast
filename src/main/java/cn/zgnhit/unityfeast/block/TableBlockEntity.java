package cn.zgnhit.unityfeast.block;

import cn.zgnhit.unityfeast.UnityFeastMod;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/** Four authoritative, one-item slots. Legacy properties are migration input and derived display only. */
public final class TableBlockEntity extends BlockEntity {
    private final NonNullList<ItemStack> slots = NonNullList.withSize(4, ItemStack.EMPTY);
    private boolean initialized;

    public TableBlockEntity(BlockPos pos, BlockState state) { super(UnityFeastMod.TABLE_ENTITY.get(), pos, state); }

    public void ensureMigrated() {
        if (initialized) return;
        initialized = true;
        var state = getBlockState();
        for (int i=0;i<4;i++)
            if (state.getValue(TableBlock.SLOTS[i])) slots.set(i, new ItemStack(UnityFeastMod.DUMPLING.get()));
        setChanged();
    }
    @Override public void onLoad() {
        super.onLoad(); ensureMigrated();
        if(level!=null&&!level.isClientSide()) {
            var old=level.getBlockState(worldPosition);var display=old;
            for(int i=0;i<4;i++)display=display.setValue(TableBlock.SLOTS[i],slots.get(i).is(UnityFeastMod.DUMPLING.get()));
            // Correct stale display flags without changing inventory or sending unchanged updates.
            if(!display.equals(old))level.setBlock(worldPosition,display,3);
        }
    }
    public ItemStack item(int slot) { ensureMigrated(); return slots.get(slot).copy(); }
    public int occupiedMask() {
        ensureMigrated(); int mask=0;
        for(int i=0;i<4;i++) if(!slots.get(i).isEmpty()) mask|=1<<i;
        return mask;
    }
    public void put(int slot, ItemStack stack) {
        ensureMigrated(); slots.set(slot, stack.isEmpty()?ItemStack.EMPTY:stack.copyWithCount(1));
        changed();
    }
    private void changed() {
        setChanged();
        if(level!=null && !level.isClientSide()) {
            var old=level.getBlockState(worldPosition); var display=old;
            for(int i=0;i<4;i++) display=display.setValue(TableBlock.SLOTS[i],slots.get(i).is(UnityFeastMod.DUMPLING.get()));
            if(!display.equals(old)) level.setBlock(worldPosition,display,3);
            level.sendBlockUpdated(worldPosition,old,display,3);
        }
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out); ensureMigrated(); out.putInt("table_data_version",1);
        for(int i=0;i<4;i++) if(!slots.get(i).isEmpty()) out.store("slot_"+i,ItemStack.CODEC,slots.get(i));
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in); initialized=in.getIntOr("table_data_version",0)>=1;
        for(int i=0;i<4;i++) slots.set(i,in.read("slot_"+i,ItemStack.CODEC).orElse(ItemStack.EMPTY).copyWithCount(1));
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
}
