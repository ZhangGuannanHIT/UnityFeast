package cn.zgnhit.unityfeast.test;

import cn.zgnhit.unityfeast.UnityFeastMod;
import java.nio.file.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Test-only bounded readback of the exact 400 chunks previously generated, after a restart. */
@EventBusSubscriber(modid=FeastGameTests.ID)
public final class WorldgenFinalCount {
    private static int chunks;private static long grass,green,fern;
    @SubscribeEvent public static void tick(ServerTickEvent.Post event)throws Exception{
        if(!System.getProperty("unity_feast.scenario","").equals("kitchen-worldgen-count"))return;
        var server=event.getServer();var level=server.overworld();
        for(int n=0;n<10&&chunks<400;n++,chunks++){
            var chunk=level.getChunk(chunks%20-10,chunks/20-10);
            for(var section:chunk.getSections())if(section.maybeHas(s->s.is(Blocks.SHORT_GRASS)||s.is(UnityFeastMod.GREENBELT)||s.is(Blocks.FERN)))
                for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++){
                    var state=section.getBlockState(x,y,z);if(state.is(Blocks.SHORT_GRASS))grass++;else if(state.is(UnityFeastMod.GREENBELT))green++;else if(state.is(Blocks.FERN))fern++;
                }
        }
        if(chunks==400){
            String row="seed,chunks,final_short_grass,final_greenbelt,final_ferns,greenbelt_share\n"+level.getSeed()+",400,"+grass+","+green+","+fern+","+(double)green/(grass+green)+"\n";
            Files.writeString(Path.of(System.getProperty("unity_feast.project"),"docs/v1.2.0/worldgen-final-"+level.getSeed()+".csv"),row);
            System.out.println("V120 FINAL CHUNK READBACK "+row);server.halt(false);
        }
    }
}
