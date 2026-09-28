package cn.zgnhit.unityfeast.test;

import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.block.TableBlock;
import cn.zgnhit.unityfeast.loot.WaterBeetleFishingModifier;
import cn.zgnhit.unityfeast.player.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(FeastGameTests.ID)
public final class FeastGameTests {
    public static final String ID = "unity_feast_tests";
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, ID);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();
    static {
        if(!System.getProperty("unity_feast.scenario","").equals("legacy")) {
        TESTS.put("upgrade_and_fish", UpdateGameTests::tableUpgrade);
        TESTS.put("new_foods_recipes", UpdateBehaviorTests::foodsAndRecipes);
        TESTS.put("mixed_food_deaths", UpdateBehaviorTests::mixedDeaths);
        TESTS.put("flight_protection", UpdateBehaviorTests::flightAndProtection);
        TESTS.put("flight_lifecycle", UpdateBehaviorTests::flightLifecycle);
        TESTS.put("rat_combat", UpdateBehaviorTests::ratCombat);
        TESTS.put("rat_jump", UpdateBehaviorTests::ratJump);
        TESTS.put("rat_spawn", RatSpawnTests::pigParity);
        TESTS.put("rat_breeding", RatLifeTests::breeding);
        TESTS.put("rat_crop_rules", RatLifeTests::cropRules);
        TESTS.put("rat_forage_navigation", RatLifeTests::forageNavigation);
        TESTS.put("rat_idle_roaming", RatLifeTests::idleRoaming);
        TESTS.put("recipes", FeastGameTests::recipes);
        TESTS.put("table_interactions_and_loot", FeastGameTests::table);
        TESTS.put("food_effects", FeastGameTests::foods);
        TESTS.put("hearts_save_load", FeastGameTests::hearts);
        TESTS.put("death_and_clone", FeastGameTests::deaths);
        TESTS.put("fishing", FeastGameTests::fishing);
        if (System.getProperty("unity_feast.scenario", "").equals("rat-spawning"))
            TESTS.keySet().removeIf(name -> !name.equals("rat_spawn") && !name.equals("rat_combat"));
        if (System.getProperty("unity_feast.scenario", "").equals("rat-life"))
            TESTS.keySet().removeIf(name -> !name.startsWith("rat_") || name.equals("rat_jump"));
        TESTS.forEach((name, test) -> FUNCTIONS.register(name, () -> test));
        }
    }
    public FeastGameTests(IEventBus bus) {
        if(System.getProperty("unity_feast.scenario","").equals("legacy")){NeoForge.EVENT_BUS.register(LegacyPreparation.class);return;}
        FUNCTIONS.register(bus);
        bus.addListener(FeastGameTests::register);
        NeoForge.EVENT_BUS.register(AcceptanceServer.class);
        NeoForge.EVENT_BUS.register(DiskUpgradeTest.class);
        NeoForge.EVENT_BUS.register(LocalAcceptanceServer.class);
    }
    private static void register(RegisterGameTestsEvent event) {
        for (String name : TESTS.keySet()) {
            var id = Identifier.fromNamespaceAndPath(ID, name);
            // A separate batch per test prevents shared gamerules and mock players interfering.
            var environment = event.registerEnvironment(id);
            event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,id),
                    new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                            name.equals("rat_idle_roaming") ? 430 : name.equals("rat_forage_navigation") ? 270 : 200, 0, true)));
        }
    }
    private static CraftingInput input(int w, Item... items) {
        return CraftingInput.of(w, items.length/w, Arrays.stream(items).map(i -> i == Items.AIR ? ItemStack.EMPTY : new ItemStack(i)).toList());
    }
    private static ItemStack craft(GameTestHelper h, CraftingInput input) {
        return h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
                .map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
    }
    private static void result(GameTestHelper h, ItemStack stack, Item item, int count) {
        h.assertTrue(stack.is(item) && stack.getCount()==count, "Expected " + item + " x " + count + "; got " + stack);
    }
    private static void recipes(GameTestHelper h) {
        for (Item wood : List.of(Items.OAK_PLANKS,Items.BIRCH_PLANKS,Items.BAMBOO_PLANKS,Items.CRIMSON_PLANKS))
            result(h,craft(h,input(3,wood,wood,wood,Items.STICK,Items.AIR,Items.STICK,Items.STICK,Items.AIR,Items.STICK)),UnityFeastMod.TABLE_ITEM.get(),1);
        h.assertTrue(craft(h,input(3,Items.OAK_PLANKS,Items.BIRCH_PLANKS,Items.OAK_PLANKS,Items.STICK,Items.AIR,Items.STICK,Items.STICK,Items.AIR,Items.STICK)).isEmpty(),"Mixed planks rejected");
        h.assertTrue(craft(h,input(3,Items.STONE,Items.STONE,Items.STONE,Items.STICK,Items.AIR,Items.STICK,Items.STICK,Items.AIR,Items.STICK)).isEmpty(),"Non-planks rejected");
        h.assertTrue(craft(h,input(3,Items.OAK_PLANKS,Items.OAK_PLANKS,Items.OAK_PLANKS,Items.STICK,Items.AIR,Items.STICK,Items.STICK,Items.AIR,Items.AIR)).isEmpty(),"Missing material rejected");
        for (int shift=0; shift<2; shift++) {
            Item[] grid=new Item[9]; Arrays.fill(grid,Items.AIR); grid[shift*3+1]=Items.PORKCHOP;
            for(int i=0;i<3;i++) grid[(shift+1)*3+i]=Items.WHEAT;
            result(h,craft(h,input(3,grid)),UnityFeastMod.DUMPLING.get(),3);
        }
        h.assertTrue(craft(h,input(3,Items.AIR,Items.COOKED_PORKCHOP,Items.AIR,Items.WHEAT,Items.WHEAT,Items.WHEAT)).isEmpty(),"Cooked pork rejected");
        Item b=UnityFeastMod.WATER_BEETLE.get();
        result(h,craft(h,input(3,b,b,b,Items.WHEAT,Items.WHEAT,Items.WHEAT)),UnityFeastMod.HOTPOT.get(),1);
        for(int a=0;a<4;a++) for(int c=0;c<4;c++) if(a!=c) {
            Item[] grid=new Item[4]; Arrays.fill(grid,Items.AIR); grid[a]=UnityFeastMod.TABLE_DUMPLING.get(); grid[c]=UnityFeastMod.HOTPOT.get();
            result(h,craft(h,input(2,grid)),UnityFeastMod.UNITY_HEART.get(),1);
        }
        var renamed=new ItemStack(UnityFeastMod.DUMPLING.get()); renamed.set(DataComponents.CUSTOM_NAME,Component.literal("桌饺"));
        h.assertTrue(craft(h,CraftingInput.of(2,2,List.of(renamed,new ItemStack(UnityFeastMod.HOTPOT.get()),ItemStack.EMPTY,ItemStack.EMPTY))).isEmpty(),"Renaming cannot replace table dumpling");
        h.succeed();
    }
    private static void table(GameTestHelper h) {
        BlockPos pos=h.absolutePos(new BlockPos(0,2,0));
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        for(int mask=0;mask<16;mask++) for(int corner=0;corner<4;corner++) {
            var state=UnityFeastMod.TABLE.get().defaultBlockState();
            for(int i=0;i<4;i++) state=state.setValue(TableBlock.SLOTS[i],(mask&(1<<i))!=0);
            h.getLevel().setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            h.getLevel().setBlock(pos,state,3);
            var hand=new ItemStack(UnityFeastMod.DUMPLING.get(),2); player.setItemInHand(InteractionHand.MAIN_HAND,hand);
            double x=(corner==0||corner==3)?.25:.75,z=(corner<2)?.25:.75;
            var hit=new BlockHitResult(new Vec3(pos.getX()+x,pos.getY()+.8,pos.getZ()+z),Direction.UP,pos,false);
            h.assertTrue(state.useItemOn(hand,h.getLevel(),player,InteractionHand.MAIN_HAND,hit).consumesAction(),"Full table must consume click");
            h.assertValueEqual(hand.getCount(),mask==15?2:1,"placement item conservation");
            h.assertValueEqual(Integer.bitCount(TableBlock.mask(h.getLevel().getBlockState(pos))),Math.min(4,Integer.bitCount(mask)+1),"table capacity");
            // Stale state passed to second action must never be authoritative.
            state.useItemOn(hand,h.getLevel(),player,InteractionHand.OFF_HAND,hit);
            h.assertValueEqual(hand.getCount(),mask==15?2:1,"offhand ignored");
            h.getLevel().setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            h.getLevel().setBlock(pos,state,3);
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            var box=new AABB(pos).inflate(2);
            h.getLevel().getEntitiesOfClass(ItemEntity.class,box).forEach(ItemEntity::discard);
            state.useWithoutItem(h.getLevel(),player,hit);
            var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,box);
            h.assertValueEqual(drops.size(),mask==0?0:1,"one extraction one entity");
            if(mask!=0) result(h,drops.getFirst().getItem(),UnityFeastMod.TABLE_DUMPLING.get(),1);
            var loot=Block.getDrops(state,h.getLevel(),pos,null,player,new ItemStack(Items.DIAMOND_AXE));
            h.assertValueEqual(loot.stream().filter(s->s.is(UnityFeastMod.TABLE_ITEM.get())).mapToInt(ItemStack::getCount).sum(),1,"one empty table");
            h.assertValueEqual(loot.stream().filter(s->s.is(UnityFeastMod.DUMPLING.get())).mapToInt(ItemStack::getCount).sum(),Integer.bitCount(mask),"ordinary dumplings on breaking");
            h.assertTrue(loot.stream().noneMatch(s->s.is(UnityFeastMod.TABLE_DUMPLING.get())),"No table dumpling from breaking");
        }
        var breaker=player(h);
        var full=UnityFeastMod.TABLE.get().defaultBlockState();
        for(var slot:TableBlock.SLOTS) full=full.setValue(slot,true);
        var box=new AABB(pos).inflate(4);
        for(int mode=0;mode<4;mode++) {
            h.getLevel().getEntitiesOfClass(ItemEntity.class,box).forEach(ItemEntity::discard);
            h.getLevel().setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            h.getLevel().setBlock(pos,full,3);
            breaker.setGameMode(mode==2?GameType.CREATIVE:GameType.SURVIVAL);
            h.getLevel().getGameRules().set(GameRules.BLOCK_DROPS,mode!=3,h.getLevel().getServer());
            var axe=new ItemStack(Items.DIAMOND_AXE);
            axe.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(mode==0?Enchantments.SILK_TOUCH:Enchantments.FORTUNE),mode==0?1:3);
            breaker.setItemInHand(InteractionHand.MAIN_HAND,axe);
            h.assertTrue(breaker.gameMode.destroyBlock(pos),"native player block destruction");
            var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,box);
            h.assertValueEqual(drops.stream().filter(e->e.getItem().is(UnityFeastMod.TABLE_ITEM.get())).mapToInt(e->e.getItem().getCount()).sum(),mode<2?1:0,"silk/fortune/creative/block_drops table conservation");
            h.assertValueEqual(drops.stream().filter(e->e.getItem().is(UnityFeastMod.DUMPLING.get())).mapToInt(e->e.getItem().getCount()).sum(),mode<2?4:0,"silk/fortune/creative/block_drops dumpling conservation");
            h.assertTrue(drops.stream().noneMatch(e->e.getItem().is(UnityFeastMod.TABLE_DUMPLING.get())),"break never produces table dumplings");
        }
        for(boolean dropsEnabled:List.of(true,false)) {
            h.getLevel().getEntitiesOfClass(ItemEntity.class,box).forEach(ItemEntity::discard);
            h.getLevel().getGameRules().set(GameRules.BLOCK_DROPS,dropsEnabled,h.getLevel().getServer());
            h.getLevel().setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            h.getLevel().setBlock(pos,full,3);
            h.getLevel().explode(null,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,3,Level.ExplosionInteraction.TNT);
            h.assertTrue(h.getLevel().getBlockState(pos).isAir(),"explosion destroys table");
            var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,box);
            int tables=drops.stream().filter(e->e.getItem().is(UnityFeastMod.TABLE_ITEM.get())).mapToInt(e->e.getItem().getCount()).sum();
            int dumplings=drops.stream().filter(e->e.getItem().is(UnityFeastMod.DUMPLING.get())).mapToInt(e->e.getItem().getCount()).sum();
            h.assertTrue(tables<=1&&dumplings<=4&&drops.stream().noneMatch(e->e.getItem().is(UnityFeastMod.TABLE_DUMPLING.get())),"explosion cannot duplicate or produce table dumplings");
            if(!dropsEnabled) h.assertTrue(drops.isEmpty(),"explosion respects block_drops=false");
        }
        h.getLevel().getGameRules().set(GameRules.BLOCK_DROPS,true,h.getLevel().getServer());
        h.succeed();
    }
    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p=h.makeMockServerPlayerInLevel();
        p.getAbilities().instabuild=false;
        p.getAbilities().invulnerable=false;
        p.connection.markClientLoaded();
        h.getLevel().getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION,false,h.getLevel().getServer());
        return p;
    }
    private static ItemStack eat(ServerPlayer p,Item item) {
        p.getFoodData().setFoodLevel(0); p.getFoodData().setSaturation(0);
        var s=new ItemStack(item,2); s.finishUsingItem(p.level(),p); return s;
    }
    private static void foods(GameTestHelper h) {
        var p=player(h);
        h.assertValueEqual(eat(p,UnityFeastMod.DUMPLING.get()).getCount(),1,"one consumed");
        h.assertValueEqual(p.getFoodData().getFoodLevel(),4,"dumpling nutrition");
        h.assertTrue(p.getActiveEffects().isEmpty(),"plain dumpling has no effects");
        p.addEffect(new MobEffectInstance(MobEffects.POISON,1000)); p.addEffect(new MobEffectInstance(MobEffects.SPEED,1000,2));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION,1000,2));
        eat(p,UnityFeastMod.TABLE_DUMPLING.get());
        h.assertValueEqual(p.getFoodData().getFoodLevel(),4,"table dumpling nutrition");
        h.assertTrue(!p.hasEffect(MobEffects.POISON)&&p.hasEffect(MobEffects.SPEED),"Remove harmful only");
        h.assertValueEqual(p.getEffect(MobEffects.ABSORPTION).getDuration(),300,"absorption duration");
        h.assertValueEqual(p.getEffect(MobEffects.REGENERATION).getAmplifier(),2,"keep higher effect");
        p.removeAllEffects(); eat(p,UnityFeastMod.WATER_BEETLE.get());
        h.assertValueEqual(p.getFoodData().getFoodLevel(),1,"beetle nutrition");
        h.assertValueEqual(p.getEffect(MobEffects.NAUSEA).getDuration(),100,"nausea duration");
        p.removeAllEffects(); var remaining=eat(p,UnityFeastMod.HOTPOT.get());
        h.assertValueEqual(p.getFoodData().getFoodLevel(),8,"hotpot nutrition");
        h.assertValueEqual(p.getEffect(MobEffects.FIRE_RESISTANCE).getDuration(),300,"fire resistance");
        h.assertValueEqual(p.getEffect(MobEffects.SLOW_FALLING).getDuration(),300,"slow falling");
        h.assertTrue(!p.hasEffect(MobEffects.NAUSEA)&&remaining.is(UnityFeastMod.HOTPOT.get()),"hotpot no nausea and no bowl");
        for(var i:List.of(UnityFeastMod.TABLE_DUMPLING.get(),UnityFeastMod.HOTPOT.get(),UnityFeastMod.UNITY_HEART.get())) {
            p.getFoodData().setFoodLevel(20);
            var s=new ItemStack(i); h.assertTrue(s.get(DataComponents.CONSUMABLE).canConsume(p,s),"can eat full");
            h.assertValueEqual(s.get(DataComponents.CONSUMABLE).consumeTicks(),32,"32 tick consumption");
        }
        p.removeAllEffects(); p.setHealth(2); p.getFoodData().setFoodLevel(1);
        var canceled=new ItemStack(UnityFeastMod.UNITY_HEART.get(),2);
        p.setItemInHand(InteractionHand.MAIN_HAND,canceled);
        canceled.use(p.level(),p,InteractionHand.MAIN_HAND);
        for(int tick=0;tick<16;tick++) p.tick();
        p.releaseUsingItem();
        h.assertValueEqual(p.getMaxHealth(),20F,"half-finished food does not increase maximum health");
        h.assertValueEqual(p.getHealth(),2F,"half-finished food does not heal");
        h.assertValueEqual(p.getFoodData().getFoodLevel(),1,"half-finished food does not restore hunger");
        h.assertValueEqual(canceled.getCount(),2,"half-finished food does not consume item");
        h.succeed();
    }
    private static void hearts(GameTestHelper h) {
        var p=player(h);
        for(int n=1;n<=3;n++) {
            p.setHealth(1); p.getFoodData().addExhaustion(30);
            eat(p,UnityFeastMod.UNITY_HEART.get());
            h.assertValueEqual(p.getMaxHealth(),20F+2*n,"new maximum health");
            h.assertValueEqual(p.getHealth(),20F+2*n,"healed after bonus");
            h.assertValueEqual(p.getFoodData().getFoodLevel(),20,"full hunger");
            h.assertValueEqual(p.getFoodData().getSaturationLevel(),20F,"full saturation");
            var food=TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING); p.getFoodData().addAdditionalSaveData(food);
            h.assertValueEqual(food.buildResult().getFloatOr("foodExhaustionLevel",-1),0F,"zero exhaustion");
        }
        p.setHealth(23); UnityHeartService.reconcile(p); UnityHeartService.reconcile(p);
        h.assertValueEqual(p.getHealth(),23F,"reconciliation must not heal");
        var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess()); p.saveWithoutId(saved);
        var copy=player(h); copy.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),saved.buildResult()));
        UnityHeartService.reconcile(copy);
        h.assertValueEqual(copy.getMaxHealth(),26F,"saved bonus");
        h.assertValueEqual(copy.getHealth(),23F,"read health above 20 without clamping");
        copy.removeAllEffects(); h.assertValueEqual(copy.getMaxHealth(),26F,"effects do not remove bonus");
        new ItemStack(Items.MILK_BUCKET).finishUsingItem(copy.level(),copy); h.assertValueEqual(copy.getMaxHealth(),26F,"milk preserves bonus");
        var origin=copy.position();
        for(var dimension:List.of(Level.NETHER,Level.END,Level.OVERWORLD)) {
            var target=h.getLevel().getServer().getLevel(dimension);
            h.assertTrue(copy.teleportTo(target,origin.x,100,origin.z,Set.of(),0,0,true),"native dimension teleport");
            h.assertValueEqual(copy.getMaxHealth(),26F,"dimension transition preserves exact bonus");
            h.assertValueEqual(copy.getHealth(),23F,"dimension transition does not heal");
        }
        h.assertTrue(UnityHeartService.next(Long.MAX_VALUE,1024)<=512,"overflow protected");
        h.succeed();
    }
    private static void deaths(GameTestHelper h) {
        for(boolean keep:List.of(false,true)) {
            var p=player(h); UnityHeartService.consume(p);
            var foreign=UnityFeastMod.id("test_foreign");
            p.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier(foreign,4,AttributeModifier.Operation.ADD_VALUE));
            h.getLevel().getGameRules().set(GameRules.KEEP_INVENTORY,keep,h.getLevel().getServer());
            p.hurtServer(p.level(),p.damageSources().genericKill(),Float.MAX_VALUE);
            System.out.println("DEATH_DEBUG keep="+keep+" health="+p.getHealth()+" rule="+p.level().getGameRules().get(GameRules.KEEP_INVENTORY)+" data="+p.getData(UnityFeastMod.HEART_DATA));
            h.getLevel().getGameRules().set(GameRules.KEEP_INVENTORY,!keep,h.getLevel().getServer());
            var save=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess()); p.saveWithoutId(save);
            var afterLogout=player(h); afterLogout.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),save.buildResult()));
            System.out.println("DEATH_DEBUG saved="+save.buildResult().get("neoforge:attachments")+" reload="+afterLogout.getData(UnityFeastMod.HEART_DATA));
            var respawn=player(h); respawn.restoreFrom(afterLogout,false);
            h.assertValueEqual(respawn.getData(UnityFeastMod.HEART_DATA).hearts(),keep?1L:0L,"death-time snapshot survives save before respawn");
            h.assertTrue(p.getAttribute(Attributes.MAX_HEALTH).getModifier(foreign)!=null,"foreign modifier untouched");
        }
        var p=player(h); UnityHeartService.consume(p); p.setHealth(21);
        var endReturn=player(h); endReturn.restoreFrom(p,true);
        h.assertValueEqual(endReturn.getMaxHealth(),22F,"non-death clone");
        h.assertValueEqual(endReturn.getHealth(),21F,"End return native restoreFrom preserves partial health above twenty");
        var event=new LivingDeathEvent(p,p.damageSources().generic());
        PlayerLifecycleEvents.died(event); event.setCanceled(true);
        h.assertValueEqual(p.getData(UnityFeastMod.HEART_DATA).effectiveHearts(),1L,"late cancellation preserves reward");
        p.setData(UnityFeastMod.HEART_DATA,new UnityHeartData(1,0));
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.TOTEM_OF_UNDYING));
        p.hurtServer(p.level(),p.damageSources().generic(),10000);
        h.assertTrue(p.isAlive() && p.getData(UnityFeastMod.HEART_DATA).effectiveHearts()==1,"totem is not death");
        h.succeed();
    }
    private static void fishing(GameTestHelper h) {
        h.assertTrue(WaterBeetleFishingModifier.replace(0)&&WaterBeetleFishingModifier.replace(Math.nextDown(.5))&&!WaterBeetleFishingModifier.replace(.5),"50% boundary");
        var rng=new Random(261278); int count=0;
        for(int i=0;i<10000;i++) if(WaterBeetleFishingModifier.replace(rng.nextDouble())) count++;
        h.assertTrue(count>4700&&count<5300,"seeded probability smoke test");
        System.out.println("UNITY_FEAST_TEST probability seed=261278 count="+count+" / 10000");
        var p=player(h); var rod=new ItemStack(Items.FISHING_ROD); p.setItemInHand(InteractionHand.MAIN_HAND,rod);
        var hook=new FishingHook(p,h.getLevel(),0,0); hook.setPos(p.getX(),p.getY(),p.getZ());
        var params=new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN,hook.position())
                .withParameter(LootContextParams.THIS_ENTITY,hook).withParameter(LootContextParams.ATTACKING_ENTITY,p)
                .withParameter(LootContextParams.TOOL,rod).create(LootContextParamSets.FISHING);
        class CountingRandom extends net.minecraft.world.level.levelgen.LegacyRandomSource {
            int calls; final double sample;
            CountingRandom(double sample) {super(1);this.sample=sample;}
            @Override public double nextDouble() {calls++;return sample;}
        }
        var modifier=new WaterBeetleFishingModifier(new LootItemCondition[0],1000);
        for(double sample:List.of(.25,.5)) {
            var random=new CountingRandom(sample);
            var context=new LootContext.Builder(params).withOptionalRandomSource(random)
                    .withQueriedLootTableId(BuiltInLootTables.FISHING.identifier()).create(Optional.empty());
            var original=new ItemStack(Items.COD,3);
            var drops=new it.unimi.dsi.fastutil.objects.ObjectArrayList<ItemStack>(); drops.add(original);
            modifier.apply(drops,context);
            h.assertValueEqual(random.calls,1,"exactly one probability draw for valid root fishing loot");
            if(sample<.5) result(h,drops.getFirst(),UnityFeastMod.WATER_BEETLE.get(),1);
            else h.assertTrue(drops.size()==1&&drops.getFirst()==original&&original.getCount()==3,"retained branch leaves original stacks untouched");
            drops.clear(); modifier.apply(drops,context);
            h.assertValueEqual(random.calls,1,"empty catch does not draw probability");
            drops.add(original);
            var subcontext=new LootContext.Builder(context).withQueriedLootTableId(Identifier.withDefaultNamespace("gameplay/fishing/fish")).create(Optional.empty());
            modifier.apply(drops,subcontext);
            h.assertValueEqual(random.calls,1,"subtable does not draw probability");
        }
        var table=h.getLevel().getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
        int actual=0;
        for(int i=1;i<=1000;i++) {
            var drops=table.getRandomItems(params,i);
            if(drops.stream().anyMatch(s->s.is(UnityFeastMod.WATER_BEETLE.get()))) {actual++; h.assertTrue(drops.size()==1&&drops.getFirst().getCount()==1,"replacement not addition");}
        }
        h.assertTrue(actual>430&&actual<570,"root table applies GLM once");
        var sub=h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.withDefaultNamespace("gameplay/fishing/fish")));
        for(int i=1;i<=100;i++) h.assertTrue(sub.getRandomItems(params,i).stream().noneMatch(s->s.is(UnityFeastMod.WATER_BEETLE.get())),"no subtable rolls");
        System.out.println("UNITY_FEAST_TEST actual fishing loot replacements="+actual+" / 1000");
        h.succeed();
    }
}
