package cn.zgnhit.unityfeast.test;

import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.entity.GreenBottle;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;

/** Real local server fixtures; registered and executed by FeastGameTests. */
public final class GreenBottleTests {
    private GreenBottleTests() {}

    public static void rulesAndLoot(GameTestHelper h) {
        var level=h.getLevel(); var f=new Fixture(h,new BlockPos(0,4,0));
        GreenBottle bottle=f.bottle(.5,.5); bottle.setNoAi(true);
        var pig=EntityType.PIG.create(level,EntitySpawnReason.COMMAND);
        h.assertValueEqual(bottle.getMaxHealth(),20F,"green bottle health 20");
        h.assertValueEqual(bottle.getAttributeValue(Attributes.MOVEMENT_SPEED),pig.getAttributeValue(Attributes.MOVEMENT_SPEED),"pig base movement");
        h.assertValueEqual(bottle.getBbHeight(),1.5F,"full 1.5 block collision height");
        h.assertValueEqual(bottle.getBbWidth(),.55F,"bottle collision width");
        h.assertTrue(bottle.maxUpStep()<1,"one block cannot be climbed by silently stepping");
        h.assertTrue(bottle.targetSelector.getAvailableGoals().isEmpty(),"no retaliation or hostile target goals");
        h.assertTrue(bottle.goalSelector.getAvailableGoals().stream().noneMatch(g->g.getGoal() instanceof BreedGoal||g.getGoal() instanceof TemptGoal||g.getGoal() instanceof FollowParentGoal),"no breeding, temptation or parent following");
        var player=f.track(UpdateGameTests.player(h));
        for(var item:List.of(Items.WHEAT,Items.CARROT,Items.POTATO,Items.BEETROOT,Items.SADDLE,Items.CARROT_ON_A_STICK)) {
            var stack=new ItemStack(item,2); player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            bottle.mobInteract(player,InteractionHand.MAIN_HAND);
            h.assertTrue(stack.getCount()==2&&!bottle.isInLove()&&!bottle.isVehicle(),"no feeding, riding or saddle interaction: "+item);
        }
        h.assertTrue(!bottle.canUseSlot(EquipmentSlot.SADDLE),"saddle slot rejected");
        bottle.setBaby(true);bottle.setAge(-24000);bottle.setInLove(player);bottle.setInLoveTime(600);
        h.assertTrue(!bottle.isBaby()&&!bottle.isInLove()&&bottle.getBreedOffspring(level,bottle)==null,"no babies or offspring from native paths");
        var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());
        bottle.saveWithoutId(saved); var tag=saved.buildResult();tag.putInt("Age",-24000);tag.putInt("InLove",600);
        var loaded=UnityFeastMod.GREEN_BOTTLE.get().create(level,EntitySpawnReason.LOAD);
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),tag));
        h.assertTrue(!loaded.isBaby()&&!loaded.isInLove(),"NBT cannot restore a baby or loving bottle");

        var table=level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,UnityFeastMod.id("entities/green_bottle")));
        var sword=new ItemStack(Items.NETHERITE_SWORD);
        sword.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING),3);
        player.setItemInHand(InteractionHand.MAIN_HAND,sword);
        var params=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,bottle)
                .withParameter(LootContextParams.ORIGIN,bottle.position()).withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().playerAttack(player))
                .withParameter(LootContextParams.ATTACKING_ENTITY,player).withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY,player)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER,player).create(LootContextParamSets.ENTITY);
        for(int result=0;result<2;result++) {
            final int branch=result;
            var fixed=new LegacyRandomSource(0){@Override public int nextInt(int bound){return Math.min(branch,bound-1);}};
            var drop=table.getRandomItems(params,fixed);
            h.assertTrue(drop.size()==1&&drop.getFirst().is(UnityFeastMod.BOTTLE_CAP.get())&&drop.getFirst().getCount()==result+1,"deterministic 1/2 loot branch with looting III");
        }
        int one=0,two=0;
        var lootRandom=RandomSource.create(0x130B0771EL);
        for(int i=1;i<=4000;i++) {
            // One seeded stream is a random sample. Adjacent Java-Random seeds have
            // correlated first draws and are not independent probability trials.
            var drop=table.getRandomItems(params,lootRandom);
            h.assertTrue(drop.size()==1&&drop.getFirst().is(UnityFeastMod.BOTTLE_CAP.get()),"only caps in single loot path");
            int count=drop.getFirst().getCount();h.assertTrue(count==1||count==2,"looting does not increase cap count");
            if(count==1)one++;else two++;
        }
        h.assertTrue(one>1800&&one<2200,"uniform one/two loot sample");
        level.getGameRules().set(GameRules.MOB_DROPS,false,level.getServer());
        bottle.hurtServer(level,level.damageSources().generic(),100);
        h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,new AABB(f.origin).inflate(2)).isEmpty(),"doMobLoot false suppresses actual death loot");
        System.out.println("V130 PASS bottle attributes/adult/passive rules; loot one="+one+", two="+two+", looting III unchanged");
        f.close();h.succeed();
    }

    public static void spawning(GameTestHelper h) {
        var level=h.getLevel();var type=UnityFeastMod.GREEN_BOTTLE.get();var f=new Fixture(h,new BlockPos(64,8,64));
        var biomes=level.registryAccess().lookupOrThrow(Registries.BIOME);
        var tag=TagKey.create(Registries.BIOME,UnityFeastMod.id("spawns_green_bottle"));
        var expected=Set.of(Biomes.PLAINS,Biomes.SUNFLOWER_PLAINS,Biomes.SNOWY_PLAINS,Biomes.SAVANNA,Biomes.SAVANNA_PLATEAU,Biomes.WINDSWEPT_SAVANNA);
        int allowed=0;
        for(var biome:biomes.listElements().toList()) {
            var entries=biome.value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream().filter(e->e.value().type()==type).toList();
            h.assertTrue(biome.is(tag)==!entries.isEmpty(),"spawn entries exactly follow mod biome tag: "+biome.key().identifier());
            if(biome.is(tag)) {
                allowed++;h.assertTrue(entries.size()==1&&entries.getFirst().weight()==10&&entries.getFirst().value().minCount()==4&&entries.getFirst().value().maxCount()==4,"pig baseline weight 10 and pack 4");
            }
        }
        for(var key:expected)h.assertTrue(biomes.getOrThrow(key).is(tag),"expected plains/savanna biome included: "+key.identifier());
        for(var key:List.of(Biomes.ICE_SPIKES,Biomes.MEADOW,Biomes.FOREST,Biomes.DESERT,Biomes.SWAMP))h.assertTrue(!biomes.getOrThrow(key).is(tag),"boundary/excluded biome absent: "+key.identifier());
        h.assertTrue(type.getCategory()==MobCategory.CREATURE,"ordinary creature cap");
        h.assertTrue(SpawnPlacements.getPlacementType(type)==SpawnPlacements.getPlacementType(EntityType.PIG),"pig ground placement");
        h.assertTrue(SpawnPlacements.getHeightmapType(type)==SpawnPlacements.getHeightmapType(EntityType.PIG),"pig heightmap");
        var dark=f.origin;
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-1;y<=3;y++)f.set(dark.offset(x,y,z),Blocks.STONE.defaultBlockState());
        f.set(dark,Blocks.AIR.defaultBlockState());f.set(dark.above(),Blocks.AIR.defaultBlockState());f.set(dark.below(),Blocks.GRASS_BLOCK.defaultBlockState());
        var chunk=ChunkPos.containing(f.origin.offset(48,16,48));int ground=f.origin.getY()+15;
        for(int x=chunk.getMinBlockX();x<=chunk.getMaxBlockX();x++)for(int z=chunk.getMinBlockZ();z<=chunk.getMaxBlockZ();z++) {
            f.set(new BlockPos(x,ground,z),Blocks.GRASS_BLOCK.defaultBlockState());
            for(int y=1;y<=4;y++)f.set(new BlockPos(x,ground+y,z),Blocks.AIR.defaultBlockState());
        }
        int finalAllowed=allowed;
        waitDark(h,dark,140,()->{
            for(var reason:List.of(EntitySpawnReason.NATURAL,EntitySpawnReason.CHUNK_GENERATION))
                h.assertTrue(SpawnPlacements.checkSpawnRules(type,level,reason,dark,RandomSource.create(1)),"zero light accepted: "+reason);
            h.assertTrue(!SpawnPlacements.checkSpawnRules(EntityType.PIG,level,EntitySpawnReason.NATURAL,dark,RandomSource.create(1)),"dark fixture really rejects ordinary pig light rule");
            f.set(dark.below(),Blocks.STONE.defaultBlockState());
            h.assertTrue(!SpawnPlacements.checkSpawnRules(type,level,EntitySpawnReason.NATURAL,dark,RandomSource.create(1)),"stone rejected");
            f.set(dark.below(),Blocks.GRASS_BLOCK.defaultBlockState());
            var body=f.bottle(.5,.5);body.setNoAi(true);
            h.assertTrue(body.checkSpawnObstruction(level)&&level.noCollision(body),"two blocks of dry space fit bottle");
            f.set(dark.above(),Blocks.STONE.defaultBlockState());
            // Mob.checkSpawnObstruction tests fluid/entity obstruction. NaturalSpawner
            // separately checks ON_GROUND and the full spawn AABB against blocks.
            h.assertTrue(!SpawnPlacements.isSpawnPositionOk(type,level,dark)&&!level.noCollision(body),"low ceiling rejects tall bottle in native spawning collision checks");
            f.set(dark.above(),Blocks.AIR.defaultBlockState());f.set(dark,Blocks.WATER.defaultBlockState());
            h.assertTrue(!body.checkSpawnObstruction(level),"liquid rejects bottle");f.set(dark,Blocks.AIR.defaultBlockState());body.discard();
            var samples=new ArrayList<Entity>();
            for(int i=0;i<MobCategory.CREATURE.getMaxInstancesPerChunk();i++) {
                var animal=type.create(level,EntitySpawnReason.NATURAL);animal.setPos(f.origin.getX(),f.origin.getY(),f.origin.getZ());samples.add(animal);
            }
            var actualChunk=level.getChunkAt(f.origin);
            var empty=NaturalSpawner.createState(289,List.of(),(pos,sink)->sink.accept(actualChunk),new LocalMobCapCalculator(level.getChunkSource().chunkMap));
            var full=NaturalSpawner.createState(289,samples,(pos,sink)->sink.accept(actualChunk),new LocalMobCapCalculator(level.getChunkSource().chunkMap));
            h.assertTrue(NaturalSpawner.getFilteredSpawningCategories(empty,true,true,true).contains(MobCategory.CREATURE),"animal cap with room allows scheduling");
            h.assertTrue(!NaturalSpawner.getFilteredSpawningCategories(full,true,true,true).contains(MobCategory.CREATURE),"actual spawn state at creature cap prevents scheduling");
            h.assertValueEqual(full.getMobCategoryCounts().getInt(MobCategory.CREATURE),samples.size(),"un-named bottles counted as normal animals");
            var bounds=new AABB(chunk.getMinBlockX(),ground,chunk.getMinBlockZ(),chunk.getMaxBlockX()+1,ground+6,chunk.getMaxBlockZ()+1);
            var random=RandomSource.create(130261278L);int bottles=0,attempts=0;
            // GameTest's environment disables natural spawning. Verify its real gate,
            // then enable it only for this bounded fixture and restore it on all exits.
            level.getGameRules().set(GameRules.SPAWN_MOBS,false,level.getServer());
            for(int i=0;i<128;i++)NaturalSpawner.spawnMobsForChunkGeneration(level,biomes.getOrThrow(Biomes.PLAINS),chunk,random);
            h.assertTrue(level.getEntitiesOfClass(Mob.class,bounds).isEmpty(),"world spawn_mobs false prevents chunk-generation animals");
            level.getGameRules().set(GameRules.SPAWN_MOBS,true,level.getServer());
            var middle=new BlockPos(chunk.getMinBlockX()+8,ground+1,chunk.getMinBlockZ()+8);
            h.assertValueEqual(level.getHeight(SpawnPlacements.getHeightmapType(type),middle.getX(),middle.getZ()),ground+1,"fixture live heightmap selects elevated grass surface");
            h.assertTrue(SpawnPlacements.isSpawnPositionOk(type,level,middle)&&level.noCollision(type.getSpawnAABB(middle.getX()+.5,middle.getY(),middle.getZ()+.5)),"fixture full loaded dry spawn space valid");
            while(bottles==0&&attempts++<1024) {
                NaturalSpawner.spawnMobsForChunkGeneration(level,biomes.getOrThrow(Biomes.PLAINS),chunk,random);
                for(var mob:level.getEntitiesOfClass(Mob.class,bounds)) {
                    if(mob.getType()==type){bottles++;h.assertTrue(!mob.isBaby(),"chunk-generated bottle adult");}
                    mob.discard();
                }
            }
            h.assertTrue(bottles>0,"actual vanilla chunk initialization creates bottles");
            // Exercise the ordinary existing-area category path separately. Unlike chunk
            // initialization this requires a real registered player at the normal distance,
            // looks up the live biome's weighted spawn list, and checks tall collision boxes.
            var candidate=new BlockPos(chunk.getMinBlockX()+8,ground+1,chunk.getMinBlockZ()+8);
            h.assertTrue(level.getBiome(candidate).is(tag),"existing-area fixture is in a permitted real biome");
            var observer=f.track(UpdateGameTests.player(h));
            observer.setPos(candidate.getX()-48.5,candidate.getY(),candidate.getZ()+.5);
            var naturalMobs=new ArrayList<Mob>();int[] naturalBottles={0};
            level.getRandom().setSeed(0x1300A11L);
            int naturalAttempts=0;
            while(naturalBottles[0]==0&&naturalAttempts++<1024) {
                // This fixture has verified empty global cap and no bottle density rule.
                // The native helper retains the weighted choice, player/spawn distances,
                // spawn placement, collision, fluid and entity-level validation untouched.
                NaturalSpawner.spawnCategoryForPosition(MobCategory.CREATURE,level,level.getChunkAt(candidate),candidate,
                        (entityType,pos,spawnChunk)->true,(mob,spawnChunk)->{
                            naturalMobs.add(mob);f.track(mob);
                            if(mob.getType()==type){naturalBottles[0]++;h.assertTrue(!mob.isBaby(),"naturally spawned bottle is adult");}
                        });
                for(var mob:naturalMobs)mob.discard();
            }
            h.assertTrue(naturalBottles[0]>0,"normal existing-area spawn path actually creates bottles with available category capacity");
            System.out.println("V130 PASS bottle spawn: biomes="+finalAllowed+", zero light valid; actual chunk attempts="+attempts+", bottles="+bottles
                    +"; existing-area attempts="+naturalAttempts+", all animals="+naturalMobs.size()+", bottles="+naturalBottles[0]+"; creature cap="+samples.size());
            f.close();h.succeed();
        });
    }

    public static void locomotion(GameTestHelper h) {
        var level=h.getLevel();var f=new Fixture(h,new BlockPos(0,4,0));f.floor(-4,32,-4,10);
        // This is a controlled native-physics comparison. Keep both subjects outside
        // the world's tick list: a wide course can straddle different entity-ticking
        // chunks, which otherwise adds a second tick for only one subject. The fixture
        // advances ServerLevel.tickNonPassenger exactly once per subject/sample below.
        // That native wrapper owns tickCount++, old transforms and NeoForge tick events;
        // calling Entity.tick directly would leave the AI age counter at zero.
        var bottle=f.track(UnityFeastMod.GREEN_BOTTLE.get().create(level,EntitySpawnReason.COMMAND));
        var pig=f.track(EntityType.PIG.create(level,EntitySpawnReason.COMMAND));
        bottle.setPos(f.origin.getX()+.5,f.origin.getY(),f.origin.getZ()+1.5);bottle.setOnGround(true);
        pig.setPos(f.origin.getX()+.5,f.origin.getY(),f.origin.getZ()+5.5);pig.setOnGround(true);
        bottle.removeFreeWill();pig.removeFreeWill();
        final double[] start={bottle.getX(),pig.getX()},walk={0,0},panic={0,0},peak={0};final boolean[] jumped={false};
        // GameTestInfo schedules callbacks in a hash map: one callback per tick is
        // necessary to order phase measurements before that tick's entity simulation.
        for(int tick=1;tick<=158;tick++){
        final int step=tick;
        h.runAtTickTime(step,()->{
        if(step==5){bottle.getNavigation().moveTo(f.origin.getX()+28.5,f.origin.getY(),f.origin.getZ()+1.5,1);pig.getNavigation().moveTo(f.origin.getX()+28.5,f.origin.getY(),f.origin.getZ()+5.5,1);}
        if(step==25){
            walk[0]=bottle.getX()-start[0];walk[1]=pig.getX()-start[1];
            System.out.println("V130 COURSE walking bottle="+walk[0]+", pig="+walk[1]+", subject ticks="+bottle.tickCount+"/"+pig.tickCount+", origin="+f.origin);
            h.assertTrue(bottle.tickCount==24&&pig.tickCount==24,"both comparison subjects receive exactly 24 native ticks before the first measurement");
            h.assertTrue(walk[0]>1&&Math.abs(walk[0]-walk[1])<.3,"actual flat walking matches pig");
            bottle.getNavigation().stop();pig.getNavigation().stop();
            bottle.setPos(f.origin.getX()+.5,f.origin.getY(),f.origin.getZ()+1.5);pig.setPos(f.origin.getX()+.5,f.origin.getY(),f.origin.getZ()+5.5);
            bottle.setDeltaMovement(Vec3.ZERO);pig.setDeltaMovement(Vec3.ZERO);start[0]=bottle.getX();start[1]=pig.getX();
            var attacker=f.track(UpdateGameTests.player(h));attacker.setPos(f.origin.getX()-3,f.origin.getY(),f.origin.getZ()+3);
            h.assertTrue(bottle.hurtServer(level,level.damageSources().playerAttack(attacker),1)&&pig.hurtServer(level,level.damageSources().playerAttack(attacker),1),"both animals accept the player hit");
            var bp=new PanicGoal(bottle,1.25);var pp=new PanicGoal(pig,1.25);
            bottle.getRandom().setSeed(13);pig.getRandom().setSeed(13);
            h.assertTrue(bottle.getLastDamageSource()!=null&&pig.getLastDamageSource()!=null
                    &&bottle.getLastDamageSource().is(net.minecraft.tags.DamageTypeTags.PANIC_CAUSES)
                    &&pig.getLastDamageSource().is(net.minecraft.tags.DamageTypeTags.PANIC_CAUSES),"actual hit has native panic-causing damage tag");
            // Native panic samples random Y offsets; an individual search can miss a
            // flat floor. Allow the same bounded retry opportunity as goal scheduling.
            boolean bottlePanic=false,pigPanic=false;
            for(int attempt=0;attempt<32&&(!bottlePanic||!pigPanic);attempt++){
                if(!bottlePanic)bottlePanic=bp.canUse();if(!pigPanic)pigPanic=pp.canUse();
            }
            h.assertTrue(bottlePanic&&pigPanic,"actual player damage activates the same vanilla PanicGoal with reachable escape space");bp.start();pp.start();
            h.assertTrue(bottle.getTarget()==null,"hurt bottle never retaliates");
            // Fix equal course endpoints after verifying goal activation, to compare speed without random path lengths.
            bottle.setDeltaMovement(Vec3.ZERO);pig.setDeltaMovement(Vec3.ZERO);
            bottle.getNavigation().moveTo(f.origin.getX()+28.5,f.origin.getY(),f.origin.getZ()+1.5,1.25);
            pig.getNavigation().moveTo(f.origin.getX()+28.5,f.origin.getY(),f.origin.getZ()+5.5,1.25);
        }
        if(step==45){
            panic[0]=bottle.getX()-start[0];panic[1]=pig.getX()-start[1];
            System.out.println("V130 COURSE fleeing bottle="+panic[0]+", pig="+panic[1]+", subject ticks="+bottle.tickCount+"/"+pig.tickCount);
            h.assertTrue(bottle.tickCount==44&&pig.tickCount==44,"both comparison subjects receive exactly 44 native ticks before the second measurement");
            h.assertTrue(panic[0]>walk[0]&&Math.abs(panic[0]-panic[1])<.35,"actual fleeing-speed course matches pig and exceeds walking");
            bottle.getNavigation().stop();pig.getNavigation().stop();pig.setNoAi(true);
            bottle.setPos(f.origin.getX()+.5,f.origin.getY(),f.origin.getZ()+1.5);bottle.setDeltaMovement(Vec3.ZERO);bottle.setOnGround(true);
            for(int z=-3;z<=9;z++)f.set(f.origin.offset(4,0,z),Blocks.STONE.defaultBlockState());
            bottle.getNavigation().moveTo(f.origin.getX()+12.5,f.origin.getY(),f.origin.getZ()+1.5,1);
        }
        if(step<155){
            level.tickNonPassenger(bottle);level.tickNonPassenger(pig);
            if(step>45){peak[0]=Math.max(peak[0],bottle.getY()-f.origin.getY());if(!bottle.onGround()&&bottle.getDeltaMovement().y>0)jumped[0]=true;}
        }
        if(step==158){
            h.assertTrue(bottle.getX()>f.origin.getX()+4.7&&peak[0]>1&&jumped[0],"bottle actually jumps across full block");
            System.out.println("V130 PASS bottle/pig measured 20-tick walking="+Arrays.toString(walk)+", flee="+Arrays.toString(panic)+", jump height="+peak[0]);
            f.close();h.succeed();
        }
        });
        }
    }

    public static void idleRoaming(GameTestHelper h) {
        var f=new Fixture(h,new BlockPos(0,4,0));f.floor(-14,14,-14,14);
        var bottle=f.bottle(.5,.5);bottle.getRandom().setSeed(261278);
        var player=f.track(UpdateGameTests.player(h));player.setPos(f.origin.getX()+3,f.origin.getY(),f.origin.getZ()+3);
        final Vec3 start=bottle.position();final double[] distance={0};
        for(int tick=1;tick<=400;tick++)h.runAtTickTime(tick,()->{
            bottle.checkDespawn();bottle.tick();h.assertTrue(bottle.getTarget()==null&&!bottle.isInLove(),"roaming is passive");
            distance[0]=Math.max(distance[0],bottle.position().subtract(start).horizontalDistanceSqr());
        });
        h.runAtTickTime(405,()->{
            h.assertTrue(distance[0]>1,"registered native idle goal actually moves bottle");
            System.out.println("V130 PASS bottle autonomous stroll distance="+Math.sqrt(distance[0]));f.close();h.succeed();
        });
    }

    private static void waitDark(GameTestHelper h,BlockPos pos,int remaining,Runnable action) {
        h.runAfterDelay(1,()->{if(h.getLevel().getRawBrightness(pos,0)==0)action.run();else {h.assertTrue(remaining>0,"dark-cell light settles");waitDark(h,pos,remaining-1,action);}});
    }
    private static final class Fixture implements GameTestListener,AutoCloseable {
        final GameTestHelper h;final ServerLevel level;final BlockPos origin;
        final Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();final List<Entity> entities=new ArrayList<>();
        final boolean mobDrops,spawnMobs;boolean closed;
        Fixture(GameTestHelper h,BlockPos relative){this.h=h;level=h.getLevel();origin=h.absolutePos(relative);mobDrops=level.getGameRules().get(GameRules.MOB_DROPS);spawnMobs=level.getGameRules().get(GameRules.SPAWN_MOBS);h.testInfo.addListener(this);}
        <T extends Entity>T track(T entity){entities.add(entity);return entity;}
        GreenBottle bottle(double x,double z){var b=track(UnityFeastMod.GREEN_BOTTLE.get().create(level,EntitySpawnReason.COMMAND));b.setPos(origin.getX()+x,origin.getY(),origin.getZ()+z);b.setOnGround(true);level.addFreshEntity(b);return b;}
        void set(BlockPos pos,BlockState state){blocks.putIfAbsent(pos.immutable(),level.getBlockState(pos));level.setBlock(pos,state,3);}
        void floor(int x0,int x1,int z0,int z1){for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++){set(origin.offset(x,-1,z),Blocks.GRASS_BLOCK.defaultBlockState());for(int y=0;y<=3;y++)set(origin.offset(x,y,z),Blocks.AIR.defaultBlockState());}}
        @Override public void close(){if(closed)return;closed=true;for(var e:entities){if(e instanceof ServerPlayer p)level.getServer().getPlayerList().remove(p);e.discard();}blocks.forEach((p,s)->level.setBlock(p,s,18));level.getGameRules().set(GameRules.MOB_DROPS,mobDrops,level.getServer());level.getGameRules().set(GameRules.SPAWN_MOBS,spawnMobs,level.getServer());}
        @Override public void testStructureLoaded(GameTestInfo info){}
        @Override public void testPassed(GameTestInfo info,GameTestRunner runner){close();}
        @Override public void testFailed(GameTestInfo info,GameTestRunner runner){close();}
        @Override public void testAddedForRerun(GameTestInfo original,GameTestInfo copy,GameTestRunner runner){close();}
    }
}
