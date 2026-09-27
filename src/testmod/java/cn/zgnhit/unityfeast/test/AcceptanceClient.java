package cn.zgnhit.unityfeast.test;

import java.nio.file.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Instrumented real Minecraft client; sends vanilla packets and captures its rendered framebuffer. */
@EventBusSubscriber(modid=FeastGameTests.ID,value=Dist.CLIENT)
public final class AcceptanceClient {
    private static String phase="";
    private static int age;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) throws Exception {
        if(!Boolean.getBoolean("unity_feast.acceptance")) return;
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.gameMode==null||mc.level==null) return;
        Path root=Path.of(System.getProperty("unity_feast.project"));
        Path file=root.resolve(".tools/acceptance/phase.txt");
        if(!Files.exists(file)) return;
        String next=Files.readString(file).trim();
        if(!next.equals(phase)) { phase=next; age=0; mc.options.keyUse.setDown(false); mc.setScreen(null); }
        age++;
        boolean first=mc.player.getName().getString().equals("FeastOne");
        if((phase.equals("PUT")||phase.equals("TAKE"))&&(age==20||age==25||age==30)) {
            var pos=new BlockPos(0,64,0);
            mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(.5,64.8125,.5),Direction.UP,pos,false));
        }
        if(phase.equals("INVENTORY")&&first&&age==15) mc.setScreen(new InventoryScreen(mc.player));
        if(phase.equals("HEARTS")) {
            int count=first?3:1;
            for(int n=0;n<count;n++) {
                if(age==20+n*55) {mc.options.keyUse.setDown(true); mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);}
                if(age==60+n*55) {mc.options.keyUse.setDown(false); mc.gameMode.releaseUsingItem(mc.player);}
            }
        }
        if(first&&age==60) {
            String filename=switch(phase) {case "EMPTY"->"01-empty-table.png";case "FULL"->"02-four-dumplings.png";case "INVENTORY"->"03-all-six-items-and-dumpling-pair.png";case "HEART_PHOTO"->"04-thirteen-hearts.png";default->null;};
            if(filename!=null) {
                Files.createDirectories(root.resolve("docs/screenshots"));
                Screenshot.grab(root.resolve("docs").toFile(),filename,mc.getMainRenderTarget(),1,c->System.out.println("ACCEPTANCE screenshot "+c.getString()));
            }
        }
        if(phase.equals("LEAVE")&&age==30) mc.stop();
    }
}
