package cn.zgnhit.unityfeast.worldgen;

import cn.zgnhit.unityfeast.UnityFeastMod;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class GreenbeltGeneration {
    private GreenbeltGeneration(){}
    /** Separate seeded stream preserves the original vegetation's RNG sequence and candidate positions. */
    public static BlockState replace(BlockState original,long seed,BlockPos pos){
        if(original==null||!original.is(Blocks.SHORT_GRASS))return original;
        long mixed=seed^pos.asLong()^0x6a09e667f3bcc909L;
        mixed=(mixed^(mixed>>>30))*0xbf58476d1ce4e5b9L;
        mixed=(mixed^(mixed>>>27))*0x94d049bb133111ebL;
        return RandomSource.create(mixed^(mixed>>>31)).nextFloat()<.2F
                ?UnityFeastMod.GREENBELT.get().getStateForAge(7):original;
    }
}
