package cn.zgnhit.unityfeast.test;

import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.block.TableBlockEntity;
import cn.zgnhit.unityfeast.entity.BottleCapProjectile;
import cn.zgnhit.unityfeast.player.SilverwingStrengthService;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Real two-client networking and framebuffer captures in a copied old test save, never a shipped class. */
@EventBusSubscriber(modid=FeastGameTests.ID)
public final class BottleScene {
    static final Path ROOT=Path.of(System.getProperty("unity_feast.project",".")),CONTROL=ROOT.resolve(".tools/tmp/v130-scene"),DOC=ROOT.resolve("docs/v1.3.0");
    static String phase="INIT";static int age,total;static long start;static int maxCaps;static boolean failed;
    static final AABB AREA=new AABB(380,60,380,440,140,440);
    static void phase(String value)throws Exception{phase=value;age=0;Files.createDirectories(CONTROL);Files.writeString(CONTROL.resolve("phase.txt"),value);System.out.println("V130 SCENE "+value);}
    static void check(boolean value,String reason){if(!value)throw new IllegalStateException(reason);System.out.println("V130 LIVE PASS "+reason);}
    static void view(ServerPlayer p,double x,double y,double z,float yaw,float pitch){p.teleportTo(p.level(),x,y,z,Set.of(),yaw,pitch,true);}
    static void wear(ServerPlayer p){p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(UnityFeastMod.SILVERWING_HELMET.get()));p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(UnityFeastMod.SILVERWING_CHESTPLATE.get()));p.setItemSlot(EquipmentSlot.LEGS,new ItemStack(UnityFeastMod.SILVERWING_LEGGINGS.get()));p.setItemSlot(EquipmentSlot.FEET,new ItemStack(UnityFeastMod.SILVERWING_BOOTS.get()));}
    @SubscribeEvent public static void started(ServerStartedEvent e)throws Exception{if(System.getProperty("unity_feast.scenario","").equals("bottle-scene")){start=System.nanoTime();phase("INIT");}}
    @SubscribeEvent public static void tick(ServerTickEvent.Post event)throws Exception{
        if(!System.getProperty("unity_feast.scenario","").equals("bottle-scene"))return;
        var server=event.getServer();var l=server.overworld();var all=server.getPlayerList().getPlayers();total++;
        if(phase.equals("LEAVE")){if(all.isEmpty()&&++age>50){server.saveEverything(false,true,true);server.halt(false);}return;}
        if(total>10000){System.out.println("V130 LIVE FAILURE scenario timeout");phase("LEAVE");return;}
        if(all.size()<2)return;
        var p=all.stream().filter(x->x.getName().getString().equals("FeastOne")).findFirst().orElseThrow();
        var q=all.stream().filter(x->x.getName().getString().equals("FeastTwo")).findFirst().orElseThrow();age++;
        try{
            switch(phase){
                case "INIT"->{if(age<80)return;
                    Files.createDirectories(DOC);Files.createDirectories(CONTROL);
                    check(l.getBlockEntity(new BlockPos(0,64,0)) instanceof TableBlockEntity t&&t.occupiedMask()==10,"old table two corners preserved in copied1.2save");
                    check(p.getData(UnityFeastMod.HEART_DATA).hearts()==3&&p.getMaxHealth()==26,"old player three hearts retain max26");
                    if(Files.exists(CONTROL.resolve("restart-ready.txt"))){
                        check(SilverwingStrengthService.isComplete(p)&&p.getEffect(MobEffects.STRENGTH).isInfiniteDuration(),"restart equipped player keeps suitI");
                        check(!SilverwingStrengthService.isComplete(q)&&q.getEffect(MobEffects.STRENGTH)!=null&&q.getEffect(MobEffects.STRENGTH).getAmplifier()==1&&q.getEffect(MobEffects.STRENGTH).getDuration()<600,"restart unequipped player retains only finite externalII");
                        view(p,408,65,414,180,10);phase("RESTART");return;
                    }
                    l.getGameRules().set(GameRules.SPAWN_MOBS,false,server);l.getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION,false,server);l.getGameRules().set(GameRules.RANDOM_TICK_SPEED,0,server);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set noon");
                    for(int x=396;x<420;x++)for(int z=396;z<420;z++){l.setBlock(new BlockPos(x,63,z),Blocks.GRASS_BLOCK.defaultBlockState(),3);for(int y=64;y<71;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);}
                    for(var player:List.of(p,q)){player.setGameMode(GameType.SURVIVAL);player.removeAllEffects();player.getInventory().clearContent();player.setHealth(player.getMaxHealth());wear(player);}
                    Item[] items={UnityFeastMod.BOTTLE_CAP.get(),UnityFeastMod.SILVERWING_HELMET.get(),UnityFeastMod.SILVERWING_CHESTPLATE.get(),UnityFeastMod.SILVERWING_LEGGINGS.get(),UnityFeastMod.SILVERWING_BOOTS.get(),UnityFeastMod.TOBACCO.get(),UnityFeastMod.CIGARETTE.get(),UnityFeastMod.XINGQING.get()};for(int i=0;i<items.length;i++)p.getInventory().setItem(i,new ItemStack(items[i]));
                    for(int i=0;i<3;i++){var b=UnityFeastMod.GREEN_BOTTLE.get().create(l,EntitySpawnReason.COMMAND);b.snapTo(403+i*2,64,405,30,0);b.setNoAi(true);l.addFreshEntity(b);}
                    var stand=new ArmorStand(l,410,64,405);stand.setYRot(180);for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))stand.setItemSlot(slot,p.getItemBySlot(slot).copy());l.addFreshEntity(stand);
                    var material=new ItemEntity(l,417,64.1,417,new ItemStack(UnityFeastMod.BOTTLE_CAP.get(),6));material.setPickUpDelay(32767);l.addFreshEntity(material);
                    view(p,408,65,413,180,13);view(q,410,64,409,180,0);phase("OVERVIEW");
                }
                case "OVERVIEW"->{if(age>140)phase("WORN");}
                case "WORN"->{if(age>140)phase("INVENTORY");}
                case "INVENTORY"->{if(age>140){p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(UnityFeastMod.CIGARETTE.get()));phase("CARTON");}}
                case "CARTON"->{if(age>140){view(p,401,64,411,-90,0);view(q,405,64,411,90,0);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(UnityFeastMod.BOTTLE_CAP.get(),2));q.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,1000,1));phase("THROW");}}
                case "THROW"->{if(age>100){check(p.getMainHandItem().getCount()==1,"realclient cap throw consumes one");check(q.getAbsorptionAmount()==4&&q.getHealth()==q.getMaxHealth(),"real PVP cap bypasses netherite but costs four absorption");check(q.hasEffect(MobEffects.SPEED)&&q.hasEffect(MobEffects.JUMP_BOOST),"real remote target receives cap benefits");new ItemStack(UnityFeastMod.DIPPED_LETTUCE.get()).finishUsingItem(l,q);phase("UNDRESS");}}
                case "UNDRESS"->{if(age>80){check(q.getItemBySlot(EquipmentSlot.HEAD).isEmpty()&&q.getEffect(MobEffects.STRENGTH).getAmplifier()==1&&q.getEffect(MobEffects.STRENGTH).getDuration()>0&&q.getEffect(MobEffects.STRENGTH).getDuration()<200,"real inventory packet removes helmet but preserves ticking lettuceII");
                    for(var player:List.of(p,q)){player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(UnityFeastMod.XINGQING.get()));player.removeAllEffects();view(player,player==p?401:405,64,411,0,-70);}phase("FIRE");}}
                case "FIRE"->{
                    int count=l.getEntitiesOfClass(BottleCapProjectile.class,AREA).size();maxCaps=Math.max(maxCaps,count);
                    if(age%100==0)System.out.println("V130 LOAD tick="+age+" caps="+count+" areaEntities="+l.getEntitiesOfClass(Entity.class,AREA).size()+" elapsedMillis="+(System.nanoTime()-start)/1000000);
                    if(age>=1200){check(maxCaps>0&&maxCaps<=24,"two clients sustained launch count remains bounded");check(p.getMainHandItem().getDamageValue()==0&&q.getMainHandItem().getDamageValue()==0,"two real clients launch without weapon wear");phase("DRAIN");}
                }
                case "DRAIN"->{if(age>240){check(l.getEntitiesOfClass(BottleCapProjectile.class,AREA).isEmpty(),"after firing stops and200ticks all caps gone");int materials=l.getEntitiesOfClass(ItemEntity.class,AREA).stream().filter(e->e.getItem().is(UnityFeastMod.BOTTLE_CAP)).mapToInt(e->e.getItem().getCount()).sum();check(materials==6,"projectiles create no recoverable caps, real material drop remains6");
                    wear(p);p.removeAllEffects();q.removeAllEffects();q.addEffect(new MobEffectInstance(MobEffects.STRENGTH,600,1));q.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);Files.writeString(CONTROL.resolve("restart-ready.txt"),"two-client fixture saved");server.saveEverything(false,true,true);phase("LEAVE");}}
                case "RESTART"->{if(age>120){check(Files.exists(CONTROL.resolve("FeastOne-sync.txt"))&&Files.exists(CONTROL.resolve("FeastTwo-sync.txt")),"both reconnecting real clients receive correct suit and external effects");p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(UnityFeastMod.BOTTLE_CAP.get()));view(p,408,64,413,180,0);phase("CAPVIEW");}}
                case "CAPVIEW"->{if(age==10){var cap=new BottleCapProjectile(l,p);cap.setPos(407.3,65.4,410.4);cap.setNoGravity(true);l.addFreshEntity(cap);}if(age>200){p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);phase("CROUCH");}}
                case "CROUCH"->{if(age>100){check(p.isCrouching(),"real client crouching pose reaches server");phase("ARMS");}}
                case "ARMS"->{if(age>100){view(p,408,64,415,180,0);phase("RUN");}}
                case "RUN"->{if(age>55){check(p.isSprinting()&&p.getZ()<415,"real client sprint and armor movement");phase("LEAVE");}}
            }
        }catch(Exception ex){failed=true;System.out.println("V130 LIVE FAILURE "+ex);ex.printStackTrace();phase("LEAVE");}
    }
}
