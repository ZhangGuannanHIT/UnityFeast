package cn.zgnhit.unityfeast.test;
import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.nio.file.*;
public final class DiskUpgradeTest {
    private static int ticks;
    @SubscribeEvent public static void tick(ServerTickEvent.Post e) throws Exception {
        if(!System.getProperty("unity_feast.scenario","").equals("upgrade")||++ticks!=30)return;
        var level=e.getServer().overworld();var marker=Path.of(System.getProperty("unity_feast.project"),".tools/v110-upgraded.txt");boolean again=Files.exists(marker);
        for(int mask=0;mask<16;mask++) {
            var pos=new BlockPos(100+(mask%4)*3,64,100+(mask/4)*3);var state=level.getBlockState(pos);
            if(!state.is(UnityFeastMod.TABLE.get()))throw new AssertionError("table ID or position lost "+mask);
            var be=(TableBlockEntity)level.getBlockEntity(pos);
            if(!again) {
                // First access is native loot evaluation; no right click or tick migration is assumed.
                var loot=Block.getDrops(state,level,pos,be);int count=loot.stream().filter(s->s.is(UnityFeastMod.DUMPLING.get())).mapToInt(ItemStack::getCount).sum();
                if(count!=Integer.bitCount(mask)||be.occupiedMask()!=mask)throw new AssertionError("disk migration lost mask "+mask+" got "+count);
                for(int i=0;i<4;i++)be.put(i,ItemStack.EMPTY);
                if(mask>=8){be.put(0,new ItemStack(Items.COD));be.put(2,new ItemStack(UnityFeastMod.DUMPLING.get()));}
            }else if(be.occupiedMask()!=(mask>=8?5:0)||(mask>=8&&!be.item(0).is(Items.COD)))throw new AssertionError("reload duplicated migration or lost mixed contents "+mask);
        }
        var chest=(ChestBlockEntity)level.getBlockEntity(new BlockPos(99,64,99));if(!chest.getItem(0).is(UnityFeastMod.UNITY_HEART.get())||chest.getItem(0).getCount()!=3)throw new AssertionError("old items lost");
        System.out.println("V110 DISK UPGRADE PASS "+(again?"reload: empty remains empty, eight mixed tables persist":"16 actual old-JAR tables migrated with native first-break loot; chest old items unchanged"));
        Files.writeString(marker,"verified");e.getServer().saveEverything(false,true,true);e.getServer().halt(false);
    }
}
