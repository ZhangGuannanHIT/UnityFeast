package cn.zgnhit.unityfeast.test;
import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.block.TableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.nio.file.*;
/** Runs against the preserved 1.0.0 main JAR, only in a copied test world. */
public final class LegacyPreparation {
    private static int ticks;
    @SubscribeEvent public static void tick(ServerTickEvent.Post e) throws Exception {
        if(!System.getProperty("unity_feast.scenario","").equals("legacy")||++ticks!=30)return;
        var level=e.getServer().overworld();
        for(int mask=0;mask<16;mask++) {
            var pos=new BlockPos(100+(mask%4)*3,64,100+(mask/4)*3);level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
            var state=UnityFeastMod.TABLE.get().defaultBlockState();for(int i=0;i<4;i++)state=state.setValue(TableBlock.SLOTS[i],(mask&(1<<i))!=0);
            level.setBlock(pos,state,3);
            if(level.getBlockEntity(pos)!=null)throw new AssertionError("fixture must be created with old non-BE table");
        }
        var chest=new BlockPos(99,64,99);level.setBlock(chest,Blocks.CHEST.defaultBlockState(),3);
        var be=(ChestBlockEntity)level.getBlockEntity(chest);be.setItem(0,new ItemStack(UnityFeastMod.UNITY_HEART.get(),3));be.setItem(1,new ItemStack(UnityFeastMod.TABLE_DUMPLING.get(),4));
        System.out.println("V110 LEGACY FIXTURE: saved all 16 old table masks with NO block entities and old chest items; copied old player files untouched");
        e.getServer().saveEverything(false,true,true);e.getServer().halt(false);
    }
}
