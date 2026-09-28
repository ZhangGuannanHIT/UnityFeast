package cn.zgnhit.unityfeast.entity;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

/** Visits planted wheat of any age; crop consumption does not feed or breed the rat. */
public final class RatEatWheatGoal extends Goal {
    private static final int SEARCH_RADIUS=15;
    private static final int PATH_CANDIDATES=8;
    private final Rat rat;
    private final double speed;
    private final Map<BlockPos,Long> unreachableUntil=new HashMap<>();
    private BlockPos target;
    private Path path;
    private long nextSearchTick;
    private long giveUpTick;
    private long nextPathTick;

    public RatEatWheatGoal(Rat rat,double speed) {
        this.rat=rat;
        this.speed=speed;
        setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));
    }

    @Override public boolean canUse() {
        if(!(rat.level() instanceof ServerLevel level)||rat.getTarget()!=null||level.getGameTime()<nextSearchTick) return false;
        long now=level.getGameTime();
        nextSearchTick=now+100+rat.getRandom().nextInt(40);
        target=null;
        path=null;
        if(!EventHooks.canEntityGrief(level,rat)) return false;
        unreachableUntil.entrySet().removeIf(entry->entry.getValue()<=now);
        Set<BlockPos> candidates=findCrops(level);
        // A rat already standing over wheat need not find a nonempty movement path.
        for(BlockPos crop:candidates) {
            if(hasArrived(level,crop)) { target=crop;return true; }
        }
        if(candidates.isEmpty()) return false;
        // One bounded multi-target search, not one path search per plant in a field.
        path=rat.getNavigation().createPath(candidates,0);
        if(path!=null&&path.canReach()) {
            target=path.getTarget();
            return true;
        }
        for(BlockPos crop:candidates) unreachableUntil.put(crop,now+1200);
        return false;
    }

    private Set<BlockPos> findCrops(ServerLevel level) {
        Vec3 origin=rat.position();
        Comparator<BlockPos> byDistance=Comparator.comparingDouble(pos->origin.distanceToSqr(Vec3.atBottomCenterOf(pos)));
        PriorityQueue<BlockPos> nearest=new PriorityQueue<>(PATH_CANDIDATES,byDistance.reversed());
        BlockPos center=rat.blockPosition();
        BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos();
        int minY=Math.max(level.getMinY()+1,center.getY()-SEARCH_RADIUS);
        int maxY=Math.min(level.getMaxY(),center.getY()+SEARCH_RADIUS);
        for(int x=center.getX()-SEARCH_RADIUS;x<=center.getX()+SEARCH_RADIUS;x++) {
            for(int z=center.getZ()-SEARCH_RADIUS;z<=center.getZ()+SEARCH_RADIUS;z++) {
                if(!level.hasChunkAt(x,z)) continue;
                for(int y=minY;y<=maxY;y++) {
                    pos.set(x,y,z);
                    if(origin.distanceToSqr(x+.5,y,z+.5)>SEARCH_RADIUS*SEARCH_RADIUS
                            ||unreachableUntil.containsKey(pos)||!isCrop(level,pos)) continue;
                    if(nearest.size()<PATH_CANDIDATES) nearest.add(pos.immutable());
                    else if(byDistance.compare(pos,nearest.peek())<0) {
                        nearest.poll();nearest.add(pos.immutable());
                    }
                }
            }
        }
        return new HashSet<>(nearest);
    }

    private boolean isCrop(ServerLevel level,BlockPos pos) {
        return level.hasChunkAt(pos)&&level.getBlockState(pos).is(Blocks.WHEAT)
                &&level.getBlockState(pos.below()).is(Blocks.FARMLAND);
    }

    private boolean hasArrived(ServerLevel level,BlockPos pos) {
        double dx=rat.getX()-(pos.getX()+.5),dz=rat.getZ()-(pos.getZ()+.5);
        if(dx*dx+dz*dz>.55*.55||Math.abs(rat.getY()-pos.getY())>.2
                ||!rat.getBoundingBox().intersects(new AABB(pos))) return false;
        return canSeeCrop(level,pos);
    }

    private boolean canSeeCrop(ServerLevel level,BlockPos pos) {
        Vec3 mouthTarget=new Vec3(pos.getX()+.5,pos.getY()+.125,pos.getZ()+.5);
        return level.clip(new ClipContext(rat.getEyePosition(),mouthTarget,
                ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,rat)).getType()==HitResult.Type.MISS;
    }

    @Override public void start() {
        long now=rat.level().getGameTime();
        giveUpTick=now+400;
        nextPathTick=now+40;
        if(path!=null) rat.getNavigation().moveTo(path,speed);
        else rat.getNavigation().stop();
    }

    @Override public boolean canContinueToUse() {
        return target!=null&&rat.level() instanceof ServerLevel level&&rat.getTarget()==null
                &&level.getGameTime()<giveUpTick&&isCrop(level,target);
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public void tick() {
        if(target==null||!(rat.level() instanceof ServerLevel level)) return;
        long now=level.getGameTime();
        if(!isCrop(level,target)||rat.getTarget()!=null||now>=giveUpTick) {
            abandon(now);return;
        }
        rat.getLookControl().setLookAt(target.getX()+.5,target.getY()+.125,target.getZ()+.5);
        if(hasArrived(level,target)) {
            BlockPos soil=target.below();
            var cropState=level.getBlockState(target);
            var soilState=level.getBlockState(soil);
            // Check both changes first, so cancelling either leaves the whole plant intact.
            if(EventHooks.canEntityGrief(level,rat)
                    &&EventHooks.onEntityDestroyBlock(rat,target,cropState)
                    &&EventHooks.onEntityDestroyBlock(rat,soil,soilState)
                    &&isCrop(level,target)&&level.destroyBlock(target,false,rat)) {
                FarmlandBlock.turnToDirt(rat,soilState,level,soil);
                nextSearchTick=now+10;
                target=null;
                rat.getNavigation().stop();
            } else abandon(now);
        } else if(rat.getNavigation().isDone()&&path!=null&&path.canReach()
                &&rat.position().distanceToSqr(Vec3.atBottomCenterOf(target))<=1.5*1.5&&canSeeCrop(level,target)) {
            // Navigation finishes within a square tolerance. Close its final diagonal gap
            // using ordinary collision-aware movement, without enlarging the bite radius.
            rat.getMoveControl().setWantedPosition(target.getX()+.5,target.getY(),target.getZ()+.5,speed);
        } else if(now>=nextPathTick) {
            nextPathTick=now+40;
            path=rat.getNavigation().createPath(target,0);
            if(path==null||!path.canReach()||!rat.getNavigation().moveTo(path,speed)) abandon(now);
        }
    }

    private void abandon(long now) {
        if(target!=null) unreachableUntil.put(target,now+1200);
        target=null;
        rat.getNavigation().stop();
    }

    @Override public void stop() {
        if(target!=null&&rat.level().getGameTime()>=giveUpTick) unreachableUntil.put(target,rat.level().getGameTime()+1200);
        target=null;
        path=null;
        rat.getNavigation().stop();
    }
}
