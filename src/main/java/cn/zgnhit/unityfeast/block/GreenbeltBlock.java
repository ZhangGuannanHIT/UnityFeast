package cn.zgnhit.unityfeast.block;

import cn.zgnhit.unityfeast.UnityFeastMod;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.FarmlandWaterManager;

/** Potato's seven growth steps, with a private dirt/grass soil equivalence. */
public final class GreenbeltBlock extends CropBlock {
    public static final MapCodec<GreenbeltBlock> CODEC = simpleCodec(GreenbeltBlock::new);
    public GreenbeltBlock(Properties p) { super(p); }
    @Override public MapCodec<GreenbeltBlock> codec() { return CODEC; }
    @Override protected ItemLike getBaseSeedId() { return UnityFeastMod.GREENBELT_ITEM.get(); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return isMaxAge(s) ? box(1,0,1,15,12,15) : box(4,0,4,12,5,12);
    }
    @Override protected boolean mayPlaceOn(BlockState s, BlockGetter l, BlockPos p) {
        return s.is(BlockTags.SUPPORTS_VEGETATION) || s.is(Blocks.FARMLAND);
    }
    @Override protected boolean canSurvive(BlockState s, LevelReader l, BlockPos p) {
        // Unlike ordinary crops, a lettuce plant does not die in low light.
        var soil=l.getBlockState(p.below());
        var decision=soil.canSustainPlant(l,p.below(),Direction.UP,s);
        return decision.isDefault()?mayPlaceOn(soil,l,p.below()):decision.isTrue();
    }
    @Override protected boolean canBeReplaced(BlockState s, BlockPlaceContext c) {
        return !c.getItemInHand().is(asItem()) && super.canBeReplaced(s,c);
    }
    @Override protected void randomTick(BlockState s, ServerLevel l, BlockPos p, RandomSource random) {
        // Water checks extend four blocks beyond each of the nine soil samples.
        if (!l.isAreaLoaded(p,5) || l.getRawBrightness(p,0)<9 || isMaxAge(s)) return;
        float speed = growthSpeed(l,p);
        if (CommonHooks.canCropGrow(l,p,s,random.nextInt((int)(25F/speed)+1)==0)) {
            l.setBlock(p,getStateForAge(getAge(s)+1),2);
            CommonHooks.fireCropGrowPost(l,p,s);
        }
    }
    public static boolean wetSoil(Level l, BlockPos p) {
        if(l.isRainingAt(p.above())) return true;
        var farmland=Blocks.FARMLAND.defaultBlockState();
        for(var q:BlockPos.betweenClosed(p.offset(-4,0,-4),p.offset(4,1,4)))
            if(l.hasChunkAt(q) && farmland.canBeHydrated(l,p,l.getFluidState(q),q)) return true;
        return FarmlandWaterManager.hasBlockWaterTicket(l,p);
    }
    public float growthSpeed(Level l, BlockPos p) {
        float speed=1;
        for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) {
            var q=p.below().offset(x,0,z);var soil=l.getBlockState(q);float contribution=0;
            if(soil.is(Blocks.FARMLAND)) contribution=soil.isFertile(l,q)?3:1;
            else if(soil.is(Blocks.DIRT)||soil.is(Blocks.GRASS_BLOCK)) contribution=wetSoil(l,q)?3:1;
            speed+=contribution/(x==0&&z==0?1:4);
        }
        boolean x=l.getBlockState(p.west()).is(this)||l.getBlockState(p.east()).is(this);
        boolean z=l.getBlockState(p.north()).is(this)||l.getBlockState(p.south()).is(this);
        if((x&&z)||l.getBlockState(p.north().west()).is(this)||l.getBlockState(p.north().east()).is(this)
                ||l.getBlockState(p.south().west()).is(this)||l.getBlockState(p.south().east()).is(this)) speed/=2;
        return speed;
    }
}
