package cn.zgnhit.unityfeast.test;

import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.block.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** One real client, vanilla network interactions, persisted fixture in a copied test world. */
@EventBusSubscriber(modid=FeastGameTests.ID)
public final class KitchenScene {
    static final BlockPos VAT=new BlockPos(300,64,306),BOARD=new BlockPos(302,64,306);
    static final Path ROOT=Path.of(System.getProperty("unity_feast.project",".")),CONTROL=ROOT.resolve(".tools/tmp/v120-scene");
    private static int age;private static String phase="INIT";private static boolean verifiedRestart;
    static void phase(String value)throws Exception{phase=value;age=0;Files.createDirectories(CONTROL);Files.writeString(CONTROL.resolve("phase.txt"),value);System.out.println("V120 SCENE "+value);}
    static void check(boolean test,String message){if(!test)throw new IllegalStateException(message);System.out.println("V120 LIVE PASS "+message);}
    static void view(ServerPlayer p,double x,double y,double z,float yaw,float pitch){p.teleportTo(p.level(),x,y,z,Set.of(),yaw,pitch,true);}
    @SubscribeEvent public static void started(ServerStartedEvent event)throws Exception{
        if(System.getProperty("unity_feast.scenario","").equals("kitchen-scene"))phase("INIT");
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event)throws Exception{
        if(!System.getProperty("unity_feast.scenario","").equals("kitchen-scene"))return;
        var server=event.getServer();var l=server.overworld();var players=server.getPlayerList().getPlayers();
        if(players.isEmpty()){if(phase.equals("LEAVE")&&++age>80){server.saveEverything(false,true,true);server.halt(false);}return;}
        var p=players.getFirst();age++;
        try {
            switch(phase){
                case "INIT"->{
                    if(age<80)return;
                    Files.createDirectories(CONTROL);
                    // Read legacy evidence before any fixture creation or player changes.
                    var legacy=l.getBlockEntity(new BlockPos(0,64,0));
                    Files.writeString(ROOT.resolve("docs/v1.2.0/upgrade-live.txt"),"old table="+(legacy instanceof TableBlockEntity table?table.saveCustomOnly(l.registryAccess()):legacy)+"\nplayer="+p.getName().getString()+" heartData="+p.getData(UnityFeastMod.HEART_DATA)+" maxHealth="+p.getMaxHealth()+" health="+p.getHealth()+" offhand="+p.getOffhandItem()+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
                    check(legacy instanceof TableBlockEntity table&&table.occupiedMask()==10,"copied old table retains its two original corner ingredients");
                    check(p.getData(UnityFeastMod.HEART_DATA).hearts()==3&&p.getMaxHealth()==26,"copied player's three old heart rewards remain exactly six health");
                    if(Files.exists(CONTROL.resolve("restart-ready.txt"))&&!verifiedRestart){
                        check(l.getBlockState(VAT).getValue(SauceVatBlock.FILL)==2,"restart vat fill=2");
                        check(l.getBlockEntity(BOARD) instanceof CuttingBoardBlockEntity b&&b.ingredient().is(Items.CHICKEN),"restart board original chicken");
                        check(l.getBlockState(new BlockPos(300,64,300)).getValue(CropBlock.AGE)==4,"restart plant age=4");
                        view(p,301.5,64,309,180,24);phase("RESTART");return;
                    }
                    l.getGameRules().set(GameRules.SPAWN_MOBS,false,server);l.getGameRules().set(GameRules.RANDOM_TICK_SPEED,0,server);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set noon");p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();
                    for(int x=297;x<=311;x++)for(int z=297;z<=312;z++){
                        l.setBlock(new BlockPos(x,63,z),Blocks.GRASS_BLOCK.defaultBlockState(),3);
                        for(int y=64;y<=69;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
                    }
                    for(int i=0;i<3;i++)l.setBlock(new BlockPos(300+i*2,64,300),UnityFeastMod.GREENBELT.get().getStateForAge(i==2?7:0),3);
                    for(int i=0;i<4;i++)l.setBlock(new BlockPos(299+i*2,64,302),UnityFeastMod.SAUCE_VAT.get().defaultBlockState().setValue(SauceVatBlock.FILL,i),3);
                    for(int i=0;i<3;i++){
                        var b=new BlockPos(300+i*2,64,304);l.setBlock(b,UnityFeastMod.CUTTING_BOARD.get().defaultBlockState().setValue(CuttingBoardBlock.FACING,Direction.NORTH),3);
                        ((CuttingBoardBlockEntity)l.getBlockEntity(b)).setIngredient(new ItemStack(List.of(Items.PORKCHOP,Items.SALMON,Items.CHICKEN).get(i)));
                    }
                    Item[] items={UnityFeastMod.GREENBELT_ITEM.get(),UnityFeastMod.SAUCE_VAT_ITEM.get(),UnityFeastMod.SOY_PASTE.get(),UnityFeastMod.DIPPED_LETTUCE.get(),UnityFeastMod.KITCHEN_KNIFE.get(),UnityFeastMod.CUTTING_BOARD_ITEM.get(),UnityFeastMod.RAW_PORK_SLICES.get(),UnityFeastMod.SALMON_SASHIMI.get(),UnityFeastMod.WHITE_CUT_CHICKEN.get()};
                    for(int i=0;i<items.length;i++)p.getInventory().setItem(i,new ItemStack(items[i]));
                    view(p,307,67,309,145,35);phase("OVERVIEW");
                }
                case "OVERVIEW"->{if(age>180){view(p,302,65,302,180,50);phase("PLANTS_VATS");}}
                case "PLANTS_VATS"->{if(age>150){view(p,302,66,305.5,180,43);phase("VATS");}}
                case "VATS"->{if(age>150){view(p,302,65,307,180,42);phase("BOARDS");}}
                case "BOARDS"->{if(age>150)phase("INVENTORY");}
                case "INVENTORY"->{if(age>150){
                    l.setBlock(VAT,UnityFeastMod.SAUCE_VAT.get().defaultBlockState(),3);l.setBlock(BOARD,UnityFeastMod.CUTTING_BOARD.get().defaultBlockState(),3);
                    p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.COCOA_BEANS,4));view(p,301.5,64,308.5,180,35);phase("FILL");}}
                case "FILL"->{if(age>130){check(l.getBlockState(VAT).getValue(SauceVatBlock.FILL)==3&&p.getMainHandItem().getCount()==1,"real client three cocoa + fourth full-vat click");p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOWL));phase("EXTRACT");}}
                case "EXTRACT"->{if(age>100){check(l.getBlockState(VAT).getValue(SauceVatBlock.FILL)==0&&p.getMainHandItem().is(UnityFeastMod.SOY_PASTE),"real client extracts one paste");p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.SALMON,2));phase("PUT");}}
                case "PUT"->{if(age>100){check(((CuttingBoardBlockEntity)l.getBlockEntity(BOARD)).ingredient().is(Items.SALMON)&&p.getMainHandItem().getCount()==1,"real client board slot and consumption");var knife=new ItemStack(UnityFeastMod.KITCHEN_KNIFE.get());knife.setDamageValue(250);p.setItemSlot(EquipmentSlot.MAINHAND,knife);phase("CUT");}}
                case "CUT"->{if(age>100){check(((CuttingBoardBlockEntity)l.getBlockEntity(BOARD)).ingredient().isEmpty()&&p.getMainHandItem().isEmpty(),"real client processing clears slot and breaks last-use knife");
                    l.setBlock(VAT,l.getBlockState(VAT).setValue(SauceVatBlock.FILL,2),3);((CuttingBoardBlockEntity)l.getBlockEntity(BOARD)).setIngredient(new ItemStack(Items.CHICKEN));l.setBlock(new BlockPos(300,64,300),UnityFeastMod.GREENBELT.get().getStateForAge(4),3);
                    Files.writeString(CONTROL.resolve("restart-ready.txt"),"saved fixture");server.saveEverything(false,true,true);phase("LEAVE");}}
                case "RESTART"->{if(age>120){check(Files.exists(CONTROL.resolve("client-restart-ok.txt")),"reconnecting client receives stored board/vat/plant state");verifiedRestart=true;phase("INIT");}}
            }
        }catch(Exception ex){System.out.println("V120 LIVE FAILURE "+ex);ex.printStackTrace();phase("LEAVE");}
    }
}
