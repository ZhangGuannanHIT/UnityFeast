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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;

public final class TableBlock extends Block {
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
        if (!stack.is(UnityFeastMod.DUMPLING.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        // Consume the interaction even when full, so holding right-click never starts eating.
        if (!level.isClientSide()) {
            BlockState actual = level.getBlockState(pos);
            if (!actual.is(this)) return InteractionResult.FAIL;
            int slot = TableSlotLogic.find(mask(actual), corner(hit, pos), false);
            if (slot >= 0) {
                level.setBlock(pos, actual.setValue(SLOTS[slot], true), UPDATE_ALL);
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
            int slot = TableSlotLogic.find(mask(actual), corner(hit, pos), true);
            if (slot < 0) return InteractionResult.PASS;
            level.setBlock(pos, actual.setValue(SLOTS[slot], false), UPDATE_ALL);
            Vec3 direction = player.position().subtract(Vec3.atCenterOf(pos)).multiply(1, 0, 1).normalize();
            ItemEntity drop = new ItemEntity(level, pos.getX()+0.5+direction.x*0.3, pos.getY()+1.05,
                    pos.getZ()+0.5+direction.z*0.3, new ItemStack(UnityFeastMod.TABLE_DUMPLING.get()));
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
