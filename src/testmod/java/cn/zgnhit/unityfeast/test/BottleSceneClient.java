package cn.zgnhit.unityfeast.test;

import java.nio.file.*;
import java.util.Set;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.ContainerInput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid=FeastGameTests.ID,value=Dist.CLIENT)
public final class BottleSceneClient {
    static String phase="";static int age;
    @SubscribeEvent public static void tick(ClientTickEvent.Post e)throws Exception{
        if(!System.getProperty("unity_feast.scenario","").equals("bottle-scene"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.gameMode==null)return;
        var root=Path.of(System.getProperty("unity_feast.project"));var control=root.resolve(".tools/tmp/v130-scene");if(!Files.exists(control.resolve("phase.txt")))return;
        var next=Files.readString(control.resolve("phase.txt")).trim();
        if(!phase.equals(next)){phase=next;age=0;mc.setScreen(null);mc.options.hideGui=false;mc.options.keyShift.setDown(false);mc.options.keyUp.setDown(false);mc.options.keySprint.setDown(false);mc.options.setCameraType(CameraType.FIRST_PERSON);}age++;
        boolean first=mc.player.getName().getString().equals("FeastOne");
        if(phase.equals("OVERVIEW")&&age==15)mc.options.hideGui=true;
        if(phase.equals("WORN")&&age==15){mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);mc.options.hideGui=true;}
        if(phase.equals("INVENTORY")&&age==15&&first)mc.setScreen(new InventoryScreen(mc.player));
        if(phase.equals("CAPVIEW")&&age==120&&first)mc.setScreen(new InventoryScreen(mc.player));
        if(phase.equals("THROW")&&age==20&&first)mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
        if(phase.equals("UNDRESS")&&age==20&&!first)mc.gameMode.handleContainerInput(0,5,0,ContainerInput.QUICK_MOVE,mc.player);
        if(phase.equals("FIRE")&&age>15)mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
        if(phase.equals("CROUCH")&&first){mc.options.keyShift.setDown(true);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);}
        if(phase.equals("ARMS")&&first&&age%20==0)mc.player.swing(InteractionHand.MAIN_HAND);
        if(phase.equals("RUN")&&first){mc.options.keyUp.setDown(true);mc.options.keySprint.setDown(true);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);}
        if((age==70&&Set.of("OVERVIEW","WORN","INVENTORY","CARTON","FIRE","RESTART","CROUCH","ARMS","CAPVIEW").contains(phase)||age==30&&phase.equals("RUN")||age==150&&phase.equals("CAPVIEW"))&&first){
            Files.createDirectories(root.resolve("docs/v1.3.0/screenshots"));Screenshot.grab(root.resolve("docs/v1.3.0").toFile(),phase.toLowerCase()+(age==150?"-inventory":"")+".png",mc.getMainRenderTarget(),1,c->System.out.println("V130 SCREENSHOT "+c.getString()));
        }
        if(phase.equals("RESTART")&&age==80){var effect=mc.player.getEffect(MobEffects.STRENGTH);if(effect!=null&&(first?effect.isInfiniteDuration():effect.getAmplifier()==1&&!effect.isInfiniteDuration()))Files.writeString(control.resolve(mc.player.getName().getString()+"-sync.txt"),"actual client effects synchronized");}
        if(phase.equals("LEAVE")&&age==10)mc.stop();
    }
}
