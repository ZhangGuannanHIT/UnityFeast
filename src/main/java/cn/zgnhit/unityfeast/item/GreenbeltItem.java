package cn.zgnhit.unityfeast.item;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class GreenbeltItem extends BlockItem {
    public GreenbeltItem(Block block, Properties p) { super(block,p); }
    @Override public InteractionResult useOn(UseOnContext c) {
        var soil=c.getLevel().getBlockState(c.getClickedPos());
        if(!soil.is(Blocks.DIRT)&&!soil.is(Blocks.GRASS_BLOCK)&&!soil.is(Blocks.FARMLAND))return InteractionResult.FAIL;
        if(c.getClickedFace()!=Direction.UP || c.getLevel().getBlockState(c.getClickedPos()).is(getBlock()))
            return InteractionResult.FAIL;
        return place(new BlockPlaceContext(c));
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        var result=super.finishUsingItem(stack,level,user);
        // Nausea is its own standard consume-effect roll; this roll is independent.
        if(level instanceof ServerLevel server && level.getDifficulty()!=Difficulty.PEACEFUL
                && user.getRandom().nextFloat()<.1F) spawnSilverfish(server,user);
        return result;
    }
    public static boolean spawnSilverfish(ServerLevel level, LivingEntity user) {
        // Entity obstruction intentionally excludes the eater, but block/fluid collisions do not.
        var fish=EntityType.SILVERFISH.create(level,EntitySpawnReason.TRIGGERED);
        if(fish==null) return false;
        double[][] offsets={{0,0},{.55,0},{-.55,0},{0,.55},{0,-.55}};
        for(var offset:offsets) {
            fish.snapTo(user.getX()+offset[0],user.getY()+.02,user.getZ()+offset[1],user.getYRot(),0);
            if(!level.hasChunkAt(fish.blockPosition()) || !level.getWorldBorder().isWithinBounds(fish.getBoundingBox())) continue;
            if(level.getBlockCollisions(fish,fish.getBoundingBox()).iterator().hasNext() || level.containsAnyLiquid(fish.getBoundingBox())) continue;
            if(!level.getEntities(fish,fish.getBoundingBox(),e->e!=user && e.isAlive() && e.canBeCollidedWith(fish)).isEmpty()) continue;
            return level.addFreshEntity(fish);
        }
        return false;
    }
}
