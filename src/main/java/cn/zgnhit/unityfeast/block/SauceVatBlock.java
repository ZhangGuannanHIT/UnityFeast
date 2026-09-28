package cn.zgnhit.unityfeast.block;

import cn.zgnhit.unityfeast.UnityFeastMod;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class SauceVatBlock extends Block {
    public static final MapCodec<SauceVatBlock> CODEC=simpleCodec(SauceVatBlock::new);
    public static final IntegerProperty FILL=IntegerProperty.create("fill",0,3);
    private static final VoxelShape SHAPE=Shapes.or(box(1,0,1,15,2,15),box(1,2,1,3,15,15),
            box(13,2,1,15,15,15),box(3,2,1,13,15,3),box(3,2,13,13,15,15));
    public SauceVatBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FILL,0));}
    @Override public MapCodec<SauceVatBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FILL);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
    @Override protected InteractionResult useItemOn(ItemStack ignored,BlockState old,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(hand!=InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        var stack=player.getMainHandItem();
        if(!stack.is(Items.COCOA_BEANS)&&!stack.is(Items.BOWL)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if(!l.isClientSide() && l.mayInteract(player,p) && player.mayBuild() && player.isWithinBlockInteractionRange(p,0)) {
            var state=l.getBlockState(p); if(!state.is(this)) return InteractionResult.FAIL;
            int fill=state.getValue(FILL);
            if(stack.is(Items.COCOA_BEANS)&&fill<3){
                l.setBlock(p,state.setValue(FILL,fill+1),3);stack.consume(1,player);
                l.playSound(null,p,SoundEvents.COMPOSTER_FILL_SUCCESS,SoundSource.BLOCKS,1,1);
            } else if(stack.is(Items.BOWL)&&fill==3){
                l.setBlock(p,state.setValue(FILL,0),3);
                var result=new ItemStack(UnityFeastMod.SOY_PASTE.get());
                if(!player.hasInfiniteMaterials()&&stack.getCount()==1) player.setItemInHand(hand,result);
                else {stack.consume(1,player);if(!player.getInventory().add(result))player.drop(result,false);}
                l.playSound(null,p,SoundEvents.BUCKET_FILL,SoundSource.BLOCKS,.7F,1);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
