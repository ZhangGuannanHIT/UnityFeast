package cn.zgnhit.unityfeast.test;

import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.block.TableBlockEntity;
import cn.zgnhit.unityfeast.entity.Rat;
import cn.zgnhit.unityfeast.player.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.nio.file.*;
import java.util.*;

/** Local two-client test only, excluded from the shipped mod. No custom rules in production. */
public final class LocalAcceptanceServer {
    static final Path CONTROL=Path.of(System.getProperty("unity_feast.project","."),".tools/v110-acceptance");
    private static String phase="WAIT";private static int age;private static boolean initialized;
    private static final BlockPos TABLE=new BlockPos(0,64,0);
    private static final List<Rat> boothRats=new ArrayList<>();
    private static Rat showcase;
    private static int naturalPeak,stableCount;private static long beginNanos;
    private static Rat raceRat;private static net.minecraft.world.entity.animal.rabbit.Rabbit raceRabbit;
    private static double racePeak;private static boolean jumped,raceStarted;
    static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);System.out.println("V110 LIVE PASS "+what);}
    static void phase(String value)throws Exception{phase=value;age=0;Files.createDirectories(CONTROL);Files.writeString(CONTROL.resolve("phase.txt"),value);System.out.println("V110 LIVE PHASE "+value);}
    static void sync(ServerPlayer p){p.inventoryMenu.broadcastChanges();p.containerMenu.broadcastChanges();}
    static void view(ServerPlayer p,double x,double y,double z,float yaw,float pitch){p.teleportTo(p.level(),x,y,z,Set.of(),yaw,pitch,true);}
    @SubscribeEvent public static void tick(ServerTickEvent.Post event)throws Exception {
        if(!System.getProperty("unity_feast.scenario","").equals("v110"))return;
        var server=event.getServer();var l=server.overworld();
        if(!initialized){initialized=true;phase(Files.exists(CONTROL.resolve("retry-natural.txt"))?"PREPARE_NATURAL":Files.exists(CONTROL.resolve("saved.txt"))?"REJOIN":"WAIT");}
        if(Files.exists(CONTROL.resolve("stop.txt"))){server.halt(false);return;}
        var p=server.getPlayerList().getPlayerByName("FeastOne");var q=server.getPlayerList().getPlayerByName("FeastTwo");
        if(phase.equals("LEAVE")&&p==null&&q==null){Files.writeString(CONTROL.resolve("saved.txt"),"40/22 hp, mixed table, captain offhand");server.saveEverything(false,true,true);server.halt(false);return;}
        if(p==null||q==null||!p.connection.hasClientLoaded()||!q.connection.hasClientLoaded())return;
        age++;
        try {
            switch(phase){
                case "PREPARE_NATURAL" -> {if(age>60){
                    // Restore open grass for the standard animal spawn rules, including on reused test worlds.
                    int roofBlocks=0;
                    for(int cx=54;cx<=70;cx++)for(int cz=54;cz<=70;cz++)if(l.getChunkSource().getChunkNow(cx,cz)!=null)
                        for(int x=0;x<16;x++)for(int z=0;z<16;z++){l.setBlock(new BlockPos(cx*16+x,-57,cz*16+z),Blocks.AIR.defaultBlockState(),3);roofBlocks++;}
                    System.out.println("V110 NATURAL cleared roof positions="+roofBlocks+" (only already-loaded chunks; no forced loading)");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set day");
                    l.getGameRules().set(GameRules.SPAWN_MOBS,false,server);view(p,1000,-60,1000,0,0);view(q,1003,-60,1000,0,0);
                    p.setData(UnityFeastMod.HEART_DATA,new UnityHeartData(0,1L,0));q.setData(UnityFeastMod.HEART_DATA,new UnityHeartData(1,0));
                    UnityHeartService.reconcile(p);UnityHeartService.reconcile(q);
                    p.setGameMode(GameType.CREATIVE);q.setGameMode(GameType.CREATIVE);p.setHealth(p.getMaxHealth());q.setHealth(q.getMaxHealth());
                    System.out.println("V110 NATURAL daylight grass trial with ordinary animal competition and caps; rewards seeded for later persistence check");phase("NATURAL");}}
                case "WAIT" -> {
                    System.out.println("V110 OLD PLAYER LOAD "+p.getMaxHealth()+"/"+p.getHealth()+" hearts="+p.getData(UnityFeastMod.HEART_DATA).hearts()+" ; "+q.getMaxHealth()+"/"+q.getHealth());
                    check(p.getMaxHealth()==26&&q.getMaxHealth()==22,"actual old player heart counts restored on login");
                    l.getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION,false,server);l.getGameRules().set(GameRules.SPAWN_MOBS,false,server);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set day");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"gamerule advance_time false");
                    for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)l.setBlock(new BlockPos(x,63,z),Blocks.SMOOTH_STONE.defaultBlockState(),3);
                    l.setBlock(TABLE,Blocks.AIR.defaultBlockState(),3);l.setBlock(TABLE,UnityFeastMod.TABLE.get().defaultBlockState(),3);
                    l.getEntitiesOfClass(ItemEntity.class,new AABB(TABLE).inflate(64)).forEach(ItemEntity::discard);
                    for(var who:List.of(p,q)){who.setGameMode(GameType.SURVIVAL);who.getInventory().clearContent();who.setData(UnityFeastMod.HEART_DATA,UnityHeartData.EMPTY);UnityHeartService.reconcile(who);who.setHealth(20);}
                    p.getInventory().setItem(0,new ItemStack(Items.COD,2));q.getInventory().setItem(0,new ItemStack(UnityFeastMod.DUMPLING.get(),2));sync(p);sync(q);
                    view(p,2.8,64,3.5,140,26);view(q,-2,64,2.8,220,26);phase("PUT");
                }
                case "PUT" -> {if(age>100){var be=(TableBlockEntity)l.getBlockEntity(TABLE);check(be.occupiedMask()==15,"two real clients fill four mixed slots");check(p.getMainHandItem().isEmpty()&&q.getMainHandItem().isEmpty(),"four inputs consumed once");phase("MIXED");}}
                case "MIXED" -> {if(age>80)phase("TAKE");}
                case "TAKE" -> {if(age>100){check(((TableBlockEntity)l.getBlockEntity(TABLE)).occupiedMask()==0,"two clients empty mixed table");
                    int fish=0,dumplings=0;for(var who:List.of(p,q))for(int i=0;i<who.getInventory().getContainerSize();i++){var s=who.getInventory().getItem(i);if(s.is(UnityFeastMod.STINKY_FISH.get()))fish+=s.getCount();if(s.is(UnityFeastMod.TABLE_DUMPLING.get()))dumplings+=s.getCount();}
                    for(var e:l.getEntitiesOfClass(ItemEntity.class,new AABB(TABLE).inflate(8))){if(e.getItem().is(UnityFeastMod.STINKY_FISH.get()))fish+=e.getItem().getCount();if(e.getItem().is(UnityFeastMod.TABLE_DUMPLING.get()))dumplings+=e.getItem().getCount();}
                    check(fish==2&&dumplings==2,"two-client exact drop conservation 2 stinky fish + 2 table dumplings");
                    for(var who:List.of(p,q))who.getInventory().clearContent();
                    p.getInventory().setItem(0,new ItemStack(UnityFeastMod.WEIJIXIAN.get()));q.getInventory().setItem(0,new ItemStack(UnityFeastMod.UNITY_HEART.get()));sync(p);sync(q);phase("EAT");}}
                case "EAT" -> {if(age>100){check(p.getMaxHealth()==40&&q.getMaxHealth()==22,"real network completed consumption independent rewards");
                    p.getInventory().clearContent();Item[] items={UnityFeastMod.STINKY_FISH.get(),UnityFeastMod.SAN_ZHI.get(),UnityFeastMod.WEIJIXIAN.get(),UnityFeastMod.CAPTAIN.get(),UnityFeastMod.UNITY_HEART.get(),UnityFeastMod.TABLE_DUMPLING.get(),UnityFeastMod.HOTPOT.get(),UnityFeastMod.WATER_BEETLE.get(),UnityFeastMod.DUMPLING.get()};
                    for(int i=0;i<items.length;i++)p.getInventory().setItem(i,new ItemStack(items[i]));sync(p);phase("ITEMS");}}
                case "ITEMS" -> {if(age>90){p.getInventory().setItem(0,ItemStack.EMPTY);sync(p);showcase=UnityFeastMod.RAT.get().create(l,EntitySpawnReason.COMMAND);showcase.snapTo(.5,64,-1.5,0,0);showcase.setNoAi(true);l.addFreshEntity(showcase);view(p,1.7,64,-.1,135,30);phase("RAT_WHITE");}}
                case "RAT_WHITE" -> {if(age>90){check(!showcase.redEyes(),"daylight neutral rat white eyes");server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set midnight");phase("RAT_RED");}}
                case "RAT_RED" -> {if(age>90){check(showcase.redEyes()&&showcase.getTarget()==null,"actual night rat red eyes but neutral");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set day");
                    for(int i=0;i<4;i++){int light=new int[]{6,7,9,10}[i];var b=new BlockPos(20+i*12,64,0);
                        for(int x=-4;x<=4;x++)for(int y=-1;y<=4;y++)for(int z=-4;z<=4;z++)l.setBlock(b.offset(x,y,z),(y==-1?Blocks.GRASS_BLOCK:y==4||Math.abs(x)==4||Math.abs(z)==4?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
                        l.setBlock(b,Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL,light),3);
                        var rat=UnityFeastMod.RAT.get().create(l,EntitySpawnReason.COMMAND);rat.snapTo(b.getX()+.5,64,b.getZ()+.5,0,0);rat.setNoAi(true);l.addFreshEntity(rat);boothRats.add(rat);
                    }
                    phase("LIGHTS");}}
                case "LIGHTS" -> {if(age>80){for(int i=0;i<4;i++){var r=boothRats.get(i);int expected=new int[]{6,7,9,10}[i];int measured=Rat.localLight(l,r.blockPosition());check(measured==expected,"actual enclosed artificial-light booth L="+measured);r.updateEyes();check(r.redEyes()==(expected<7),"synced red threshold L="+expected);check(Rat.canSpawn(UnityFeastMod.RAT.get(),l,EntitySpawnReason.NATURAL,r.blockPosition(),l.getRandom())==(expected>8),"standard animal spawn predicate on actual grass/light L="+expected);}
                    view(p,2.8,64,3.5,140,26);p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(UnityFeastMod.CAPTAIN.get()));sync(p);phase("FLIGHT");}}
                case "FLIGHT" -> {if(age>130){check(p.mayFly()&&p.getAbilities().flying&&p.getY()>65,"double-jump standard client flight accepted by dedicated server allow-flight=false");check(!q.mayFly(),"second player receives no flight");p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);sync(p);phase("REMOVE");}}
                case "REMOVE" -> {if(age>30){check(!p.mayFly()&&!p.getAbilities().flying,"offhand removal revokes only owner's flight");p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(UnityFeastMod.CAPTAIN.get()));sync(p);
                    var be=(TableBlockEntity)l.getBlockEntity(TABLE);be.put(0,new ItemStack(Items.SALMON));be.put(2,new ItemStack(UnityFeastMod.DUMPLING.get()));
                    for(var r:boothRats)r.discard();showcase.discard();
                    view(p,1000,-60,1000,0,0);view(q,1003,-60,1000,0,0);server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set midnight");phase("NATURAL");}}
                case "NATURAL" -> {
                    if(age==100){for(var e:l.getAllEntities())if(e instanceof Mob m&&m.getType().getCategory()==MobCategory.CREATURE)m.discard();l.getGameRules().set(GameRules.SPAWN_MOBS,true,server);beginNanos=System.nanoTime();System.out.println("V110 NATURAL biome="+l.getBiome(p.blockPosition()).getRegisteredName()+" light="+Rat.localLight(l,p.blockPosition())+" ground="+l.getBlockState(p.blockPosition().below())+" list="+l.getBiome(p.blockPosition()).value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap());}
                    if(age>=120&&age%100==0){int rats=0,total=0;for(var e:l.getAllEntities()){total++;if(e instanceof Rat)rats++;}naturalPeak=Math.max(naturalPeak,rats);var state=l.getChunkSource().getLastSpawnState();System.out.println("V110 NATURAL tick="+age+" rats="+rats+" entities="+total+" categories="+(state==null?"none":state.getMobCategoryCounts())+" spawnableChunks="+(state==null?0:state.getSpawnableChunkCount()));}
                    if(age==1800){check(naturalPeak>0,"rats appeared through ordinary server natural spawning, no summon/test spawner");
                        // Administrator-created cap fixture; deliberately distinct from the natural-spawn observation.
                        for(int i=0;i<20;i++){var r=UnityFeastMod.RAT.get().create(l,EntitySpawnReason.COMMAND);r.snapTo(1005+i%4,-60,1005+i/4,0,0);r.setNoAi(true);l.addFreshEntity(r);}phase("CAP");}
                }
                case "CAP" -> {
                    if(age==60){var state=l.getChunkSource().getLastSpawnState();check(state!=null&&!NaturalSpawner.getFilteredSpawningCategories(state,true,true,true).contains(MobCategory.CREATURE),"CREATURE cap excludes natural category");stableCount=0;for(var e:l.getAllEntities())if(e instanceof Rat)stableCount++;}
                    if(age%200==0){int count=0,total=0;for(var e:l.getAllEntities()){total++;if(e instanceof Rat)count++;}System.out.println("V110 CAP tick="+age+" rats="+count+" entities="+total+" averageTickNanos="+server.getAverageTickTimeNanos());check(count<=stableCount,"no additional rats while category full");}
                    if(age>=1000){l.getGameRules().set(GameRules.SPAWN_MOBS,false,server);Files.deleteIfExists(CONTROL.resolve("retry-natural.txt"));view(p,2.8,64,3.5,140,26);view(q,-2,64,2.8,220,26);p.setGameMode(GameType.SURVIVAL);q.setGameMode(GameType.SURVIVAL);p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(UnityFeastMod.CAPTAIN.get()));q.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.removeAllEffects();q.removeAllEffects();p.setHealth(31);q.setHealth(17);phase("LEAVE");}
                }
                case "REJOIN" -> {if(age>80){check(p.getMaxHealth()==40&&q.getMaxHealth()==22&&p.getHealth()==31&&q.getHealth()==17,"server restart restores independent reward without healing");check(p.mayFly()&&!q.mayFly(),"restart restores offhand flight only to holder");var b=(TableBlockEntity)l.getBlockEntity(TABLE);check(b.occupiedMask()==5&&b.item(0).is(Items.SALMON),"restart preserves mixed table");
                    for(int x=199;x<=213;x++)for(int z=200;z<=205;z++){l.setBlock(new BlockPos(x,63,z),Blocks.STONE.defaultBlockState(),3);for(int y=64;y<=67;y++)l.setBlock(new BlockPos(x,y,z),x==204&&y==64?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);}
                    raceRat=UnityFeastMod.RAT.get().create(l,EntitySpawnReason.COMMAND);raceRabbit=EntityType.RABBIT.create(l,EntitySpawnReason.COMMAND);
                    raceRat.snapTo(200.5,64,201.5,0,0);raceRabbit.snapTo(200.5,64,203.5,0,0);raceRat.removeFreeWill();raceRabbit.removeFreeWill();l.addFreshEntity(raceRat);l.addFreshEntity(raceRabbit);
                    view(p,202,64,205.5,180,20);view(q,201,64,205.5,180,20);racePeak=64;phase("RACE");}}
                case "RACE" -> {
                    if(!raceStarted&&raceRat.onGround()&&raceRabbit.onGround()){
                        boolean r=raceRat.getNavigation().moveTo(211.5,64,201.5,.6),b=raceRabbit.getNavigation().moveTo(211.5,64,203.5,.6);
                        if(r&&b){raceStarted=true;age=0;System.out.println("V110 LIVE COURSE both paths ready after chunk load");}
                    }
                    racePeak=Math.max(racePeak,raceRat.getY());if(!raceRat.onGround()&&raceRat.getDeltaMovement().y>0)jumped=true;
                    if(age%40==0)System.out.println("V110 LIVE COURSE tick="+age+" ratDistance="+(raceRat.getX()-200.5)+" rabbitDistance="+(raceRabbit.getX()-200.5)+" peak="+(racePeak-64)+" entityTick="+raceRat.tickCount+" ground="+raceRat.onGround()+" pathReady="+raceStarted);
                    if(age>185){check(raceRat.getX()>205&&racePeak>65&&jumped,"naturally ticking server rat jumps full obstacle without manual physics ticks");raceRat.discard();raceRabbit.discard();phase("DONE");}
                }
                case "DONE" -> {if(age>100){Files.writeString(CONTROL.resolve("complete.txt"),"passed");phase("LEAVE");}}
            }
        }catch(Throwable failure){failure.printStackTrace();Files.writeString(CONTROL.resolve("failure.txt"),phase+": "+failure);phase("FAILED");}
    }
}
