package cn.zgnhit.unityfeast.test;
import java.nio.file.*;
import cn.zgnhit.unityfeast.block.TableBlockEntity;
import cn.zgnhit.unityfeast.entity.Rat;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid=FeastGameTests.ID,value=Dist.CLIENT)
public final class LocalAcceptanceClient {
    private static String phase="";private static int age;
    @SubscribeEvent public static void tick(ClientTickEvent.Post e)throws Exception {
        if(!System.getProperty("unity_feast.scenario","").equals("v110"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.gameMode==null)return;
        var root=Path.of(System.getProperty("unity_feast.project"));var control=root.resolve(".tools/v110-acceptance");var file=control.resolve("phase.txt");if(!Files.exists(file))return;
        String next=Files.readString(file).trim();if(!next.equals(phase)){phase=next;age=0;mc.options.keyUse.setDown(false);mc.options.keyJump.setDown(false);mc.setScreen(null);}
        age++;boolean first=mc.player.getName().getString().equals("FeastOne");
        if((phase.equals("PUT")||phase.equals("TAKE"))&&(age==20||age==35)){
            mc.player.getInventory().setSelectedSlot(phase.equals("TAKE")?(age==20?8:7):0);
            mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(.5,64.8125,.5),Direction.UP,new BlockPos(0,64,0),false));
        }
        if(phase.equals("EAT")){if(age==15){mc.player.getInventory().setSelectedSlot(0);mc.options.keyUse.setDown(true);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);}if(age==60){mc.options.keyUse.setDown(false);mc.gameMode.releaseUsingItem(mc.player);}}
        if(phase.equals("ITEMS")&&first&&age==15)mc.setScreen(new InventoryScreen(mc.player));
        if(phase.equals("FLIGHT")&&first){if(age==20||age==24)mc.options.keyJump.setDown(true);if(age==22||age==65)mc.options.keyJump.setDown(false);}
        if(age==55&&phase.equals("MIXED")){var be=(TableBlockEntity)mc.level.getBlockEntity(new BlockPos(0,64,0));if(be==null||be.occupiedMask()!=15)throw new AssertionError("client mixed table desync");System.out.println("V110 CLIENT PASS mixed table synced "+mc.player.getName().getString());}
        if(age==55&&(phase.equals("RAT_RED")||phase.equals("RAT_WHITE")))for(var ent:mc.level.entitiesForRendering())if(ent instanceof Rat r)System.out.println("V110 CLIENT rat id="+r.getId()+" red="+r.redEyes()+" phase="+phase);
        if(first&&age==70){String name=switch(phase){case "MIXED"->"01-mixed-table.png";case "ITEMS"->"02-new-items.png";case "RAT_WHITE"->"03-rat-white-eyes.png";case "RAT_RED"->"04-rat-emissive-red-eyes.png";case "FLIGHT"->"05-captain-flight.png";case "REJOIN"->"06-restart.png";case "RACE"->"07-rat-rabbit-course.png";default->null;};
            if(name!=null){Files.createDirectories(root.resolve("docs/v1.1.0/screenshots"));Screenshot.grab(root.resolve("docs/v1.1.0").toFile(),name,mc.getMainRenderTarget(),1,c->System.out.println("V110 SCREENSHOT "+c.getString()));}}
        if((phase.equals("LEAVE")||phase.equals("FAILED"))&&age==30)mc.stop();
    }
}
