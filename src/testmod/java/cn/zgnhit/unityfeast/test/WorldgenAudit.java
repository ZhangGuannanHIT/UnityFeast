package cn.zgnhit.unityfeast.test;

import cn.zgnhit.unityfeast.UnityFeastMod;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid=FeastGameTests.ID)
public final class WorldgenAudit {
    private static final Map<String,LongAdder[]> counts=new ConcurrentHashMap<>();
    private static int chunks;
    public static void placed(WorldGenRegion l,BlockPos p,BlockState s){
        if(!System.getProperty("unity_feast.scenario","").equals("kitchen-worldgen"))return;
        int index=s.is(Blocks.SHORT_GRASS)?0:s.is(UnityFeastMod.GREENBELT)?1:s.is(Blocks.FERN)?2:-1;
        if(index<0)return;
        String biome=l.getBiome(p).unwrapKey().orElseThrow().identifier().toString();
        var row=counts.computeIfAbsent(biome,k->new LongAdder[]{new LongAdder(),new LongAdder(),new LongAdder()});row[index].increment();
        if(index==1&&s.getValue(CropBlock.AGE)!=7)throw new IllegalStateException("generated immature greenbelt");
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post e)throws Exception{
        if(!System.getProperty("unity_feast.scenario","").equals("kitchen-worldgen"))return;
        var l=e.getServer().overworld();
        // Bounded audit-only terrain generation. No such loading exists in the shipped mod.
        int x=(chunks%20)-10,z=(chunks/20)-10;l.getChunk(x,z);chunks++;
        if(chunks%40==0)System.out.println("V120 WORLDGEN sample chunks="+chunks+" placements="+counts.values().stream().mapToLong(a->a[0].sum()+a[1].sum()).sum());
        if(chunks==400){
            StringBuilder report=new StringBuilder("seed="+l.getSeed()+"; sampled 20x20 requested chunks; observations include dependency decoration\nbiome,original_short_candidates,replaced,short_grass,ferns,ratio\n");
            long total=0,green=0;
            for(var entry:new TreeMap<>(counts).entrySet()){
                long s=entry.getValue()[0].sum(),g=entry.getValue()[1].sum(),f=entry.getValue()[2].sum();total+=s+g;green+=g;
                report.append(entry.getKey()+","+(s+g)+","+g+","+s+","+f+","+(s+g==0?0:(double)g/(s+g))+"\n");
            }
            report.append("TOTAL,"+total+","+green+","+(total-green)+",,"+(double)green/total+"\n");
            var dir=Path.of(System.getProperty("unity_feast.project"),"docs/v1.2.0");Files.createDirectories(dir);
            Files.writeString(dir.resolve("worldgen-"+l.getSeed()+".csv"),report.toString());System.out.println("V120 WORLDGEN REAL "+report);
            e.getServer().saveEverything(false,true,true);e.getServer().halt(false);
        }
    }
}
