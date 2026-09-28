package cn.zgnhit.unityfeast.block;

import cn.zgnhit.unityfeast.UnityFeastMod;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;

public final class TableBlock extends Block implements EntityBlock {
    public static final MapCodec<TableBlock> CODEC = simpleCodec(TableBlock::new);
    public static final BooleanProperty[] SLOTS = {
            BooleanProperty.create("dumpling_nw"), BooleanProperty.create("dumpling_ne"),
            BooleanProperty.create("dumpling_se"), BooleanProperty.create("dumpling_sw")};
    private static final VoxelShape SHAPE = Shapes.or(box(0,11,0,16,13,16),
            box(1,0,1,4,11,4), box(12,0,1,15,11,4), box(1,0,12,4,11,15), box(12,0,12,15,11,15));
    public TableBlock(Properties p) {
        super(p);
        BlockState state = stateDefinition.any();
        for (var slot : SLOTS) state = state.setValue(slot, false);
        registerDefaultState(state);
    }
    @Override public MapCodec<TableBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TableBlockEntity(pos,state); }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var entity=params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        params.withDynamicDrop(UnityFeastMod.id("table_contents"), out -> {
            for(int i=0;i<4;i++) {
                ItemStack stack=entity instanceof TableBlockEntity table ? table.item(i)
                        : state.getValue(SLOTS[i])?new ItemStack(UnityFeastMod.DUMPLING.get()):ItemStack.EMPTY;
                if(!stack.isEmpty()) out.accept(stack);
            }
        });
        return super.getDrops(state,params);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(SLOTS); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    public static int mask(BlockState state) {
        int mask = 0;
        for (int i = 0; i < 4; i++) if (state.getValue(SLOTS[i])) mask |= 1 << i;
        return mask;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState ignored, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if(stack.is(UnityFeastMod.TABLE_DUMPLING.get())||stack.is(UnityFeastMod.STINKY_FISH.get()))return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!stack.is(UnityFeastMod.DUMPLING.get()) && !stack.is(UnityFeastMod.RAW_FISH)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        // Consume the interaction even when full, so holding right-click never starts eating.
        if (!level.isClientSide()) {
            BlockState actual = level.getBlockState(pos);
            if (!actual.is(this)) return InteractionResult.FAIL;
            if(!(level.getBlockEntity(pos) instanceof TableBlockEntity table)) return InteractionResult.FAIL;
            int slot = TableSlotLogic.find(table.occupiedMask(), corner(hit, pos), false);
            if (slot >= 0) {
                table.put(slot,stack);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.6F, 1.3F);
            }
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState ignored, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Vanilla invokes this fallback for the main hand only. Check the real hand too.
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            BlockState actual = level.getBlockState(pos);
            if (!actual.is(this)) return InteractionResult.PASS;
            if(!(level.getBlockEntity(pos) instanceof TableBlockEntity table)) return InteractionResult.PASS;
            int slot = TableSlotLogic.find(table.occupiedMask(), corner(hit, pos), true);
            if (slot < 0) return InteractionResult.PASS;
            var result=table.item(slot).is(UnityFeastMod.DUMPLING.get())?UnityFeastMod.TABLE_DUMPLING:UnityFeastMod.STINKY_FISH;
            table.put(slot,ItemStack.EMPTY);
            Vec3 direction = player.position().subtract(Vec3.atCenterOf(pos)).multiply(1, 0, 1).normalize();
            ItemEntity drop = new ItemEntity(level, pos.getX()+0.5+direction.x*0.3, pos.getY()+1.05,
                    pos.getZ()+0.5+direction.z*0.3, new ItemStack(result.get()));
            drop.setDeltaMovement(direction.x*0.12, 0.12, direction.z*0.12);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
        return InteractionResult.SUCCESS;
    }
    private static int corner(BlockHitResult hit, BlockPos pos) {
        return TableSlotLogic.corner(hit.getLocation().x-pos.getX(), hit.getLocation().z-pos.getZ());
    }
}
