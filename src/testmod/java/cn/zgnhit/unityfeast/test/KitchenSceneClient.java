package cn.zgnhit.unityfeast.test;

import cn.zgnhit.unityfeast.block.*;
import java.nio.file.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid=FeastGameTests.ID,value=Dist.CLIENT)
public final class KitchenSceneClient {
    private static String phase="";private static int age;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event)throws Exception{
        if(!System.getProperty("unity_feast.scenario","").equals("kitchen-scene"))return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.gameMode==null)return;
        var root=Path.of(System.getProperty("unity_feast.project"));var control=root.resolve(".tools/tmp/v120-scene");
        if(!Files.exists(control.resolve("phase.txt")))return;
        String next=Files.readString(control.resolve("phase.txt")).trim();if(!phase.equals(next)){phase=next;age=0;mc.setScreen(null);mc.options.hideGui=false;}age++;
        BlockPos pos=phase.equals("FILL")||phase.equals("EXTRACT")?new BlockPos(300,64,306):new BlockPos(302,64,306);
        if((phase.equals("FILL")&&(age==20||age==35||age==50||age==65))||((phase.equals("EXTRACT")||phase.equals("PUT")||phase.equals("CUT"))&&age==20))
            mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        if(phase.equals("INVENTORY")&&age==15)mc.setScreen(new InventoryScreen(mc.player));
        if((phase.equals("OVERVIEW")||phase.equals("PLANTS_VATS")||phase.equals("VATS")||phase.equals("BOARDS"))&&age==20)mc.options.hideGui=true;
        if(age==80&&java.util.Set.of("OVERVIEW","PLANTS_VATS","VATS","BOARDS","INVENTORY","PUT","RESTART").contains(phase)){
            Files.createDirectories(root.resolve("docs/v1.2.0/screenshots"));Screenshot.grab(root.resolve("docs/v1.2.0").toFile(),phase.toLowerCase()+".png",mc.getMainRenderTarget(),1,c->System.out.println("V120 SCREENSHOT "+c.getString()));
        }
        if(phase.equals("RESTART")&&age==60){
            var vat=new BlockPos(300,64,306);var board=new BlockPos(302,64,306);var plant=new BlockPos(300,64,300);
            if(mc.level.getBlockState(vat).getValue(SauceVatBlock.FILL)==2&&mc.level.getBlockEntity(board) instanceof CuttingBoardBlockEntity b&&b.ingredient().is(Items.CHICKEN)&&mc.level.getBlockState(plant).getValue(CropBlock.AGE)==4)
                Files.writeString(control.resolve("client-restart-ok.txt"),"actual client synchronized after restart");
        }
        if(phase.equals("LEAVE")&&age==20)mc.stop();
    }
}
