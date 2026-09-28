package cn.zgnhit.unityfeast.mixin;

import cn.zgnhit.unityfeast.worldgen.GreenbeltGeneration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Narrow 26.1 vegetation hook; ServerLevel bone meal and player placement are excluded. */
@Mixin(SimpleBlockFeature.class)
public abstract class SimpleBlockFeatureMixin {
    @Redirect(method="place",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/levelgen/feature/stateproviders/BlockStateProvider;getOptionalState(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState unityFeast$shortGrass(BlockStateProvider provider,WorldGenLevel level,RandomSource random,BlockPos pos){
        BlockState original=provider.getOptionalState(level,random,pos);
        return level instanceof WorldGenRegion region ? GreenbeltGeneration.replace(original,region.getSeed(),pos) : original;
    }
}
