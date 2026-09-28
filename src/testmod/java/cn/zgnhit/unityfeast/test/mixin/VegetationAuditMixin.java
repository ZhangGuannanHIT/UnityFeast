package cn.zgnhit.unityfeast.test.mixin;

import cn.zgnhit.unityfeast.test.WorldgenAudit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Test JAR only: observe successful feature placements, without changing state, RNG or result. */
@Mixin(SimpleBlockFeature.class)
public abstract class VegetationAuditMixin {
    @Redirect(method="place",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean audit(WorldGenLevel level,BlockPos pos,BlockState state,int flags){
        boolean placed=level.setBlock(pos,state,flags);
        if(placed&&level instanceof WorldGenRegion region)WorldgenAudit.placed(region,pos,state);
        return placed;
    }
}
