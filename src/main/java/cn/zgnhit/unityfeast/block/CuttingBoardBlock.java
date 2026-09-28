package cn.zgnhit.unityfeast.block;

import cn.zgnhit.unityfeast.UnityFeastMod;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class CuttingBoardBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<CuttingBoardBlock> CODEC=simpleCodec(CuttingBoardBlock::new);
    private static final VoxelShape NS=box(2,0,1,14,2,15), EW=box(1,0,2,15,2,14);
    public CuttingBoardBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override public MapCodec<CuttingBoardBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new CuttingBoardBlockEntity(p,s);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(FACING).getAxis()==Direction.Axis.X?EW:NS;}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){return l.getBlockState(p.below()).isFaceSturdy(l,p.below(),Direction.UP);}
    @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos q,BlockState neighbor,RandomSource random){
        return !canSurvive(s,l,p)?Blocks.AIR.defaultBlockState():super.updateShape(s,l,ticks,p,d,q,neighbor,random);
    }
    @Override protected List<ItemStack> getDrops(BlockState s,LootParams.Builder params){
        var be=params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        params.withDynamicDrop(UnityFeastMod.id("board_contents"),out->{
            if(be instanceof CuttingBoardBlockEntity board&&!board.ingredient().isEmpty())out.accept(board.ingredient());
        });
        return super.getDrops(s,params);
    }
    private boolean allowed(Level l,BlockPos p,Player player){return l.mayInteract(player,p)&&player.mayBuild()&&player.isWithinBlockInteractionRange(p,0);}
    @Override protected InteractionResult useItemOn(ItemStack ignored,BlockState state,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        var held=player.getMainHandItem();
        if(!CuttingBoardBlockEntity.accepts(held)&&!held.is(UnityFeastMod.KITCHEN_KNIFE))return InteractionResult.TRY_WITH_EMPTY_HAND;
        if(!l.isClientSide()&&allowed(l,p,player)&&l.getBlockState(p).is(this)&&l.getBlockEntity(p) instanceof CuttingBoardBlockEntity board){
            var input=board.ingredient();
            if(held.is(UnityFeastMod.KITCHEN_KNIFE)){
                var output=resultFor(input);
                if(!output.isEmpty()){
                    board.removeIngredient();
                    drop(l,p,output);
                    held.hurtAndBreak(1,player,EquipmentSlot.MAINHAND);
                    l.playSound(null,p,SoundEvents.WOOD_BREAK,SoundSource.BLOCKS,.6F,1.5F);
                }
            }else if(input.isEmpty()){
                board.setIngredient(held);held.consume(1,player);
                l.playSound(null,p,SoundEvents.WOOD_PLACE,SoundSource.BLOCKS,.5F,1.2F);
            }
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(!player.getMainHandItem().isEmpty())return InteractionResult.PASS;
        if(!l.isClientSide()&&allowed(l,p,player)&&l.getBlockState(p).is(this)&&l.getBlockEntity(p) instanceof CuttingBoardBlockEntity board){
            var input=board.removeIngredient();if(!input.isEmpty())drop(l,p,input);
        }
        return InteractionResult.SUCCESS;
    }
    public static ItemStack resultFor(ItemStack raw){
        if(raw.is(Items.PORKCHOP))return new ItemStack(UnityFeastMod.RAW_PORK_SLICES.get());
        if(raw.is(Items.SALMON))return new ItemStack(UnityFeastMod.SALMON_SASHIMI.get(),2);
        if(raw.is(Items.CHICKEN))return new ItemStack(UnityFeastMod.WHITE_CUT_CHICKEN.get());
        return ItemStack.EMPTY;
    }
    private static void drop(Level l,BlockPos p,ItemStack s){
        var item=new ItemEntity(l,p.getX()+.5,p.getY()+.25,p.getZ()+.5,s);item.setDeltaMovement(0,.1,0);
        item.setDefaultPickUpDelay();l.addFreshEntity(item);
    }
}
