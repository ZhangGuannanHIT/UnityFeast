package cn.zgnhit.unityfeast.test;

import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.block.*;
import cn.zgnhit.unityfeast.item.GreenbeltItem;
import cn.zgnhit.unityfeast.worldgen.GreenbeltGeneration;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.*;

/** Tests actual registered objects and server interactions, separate from graphical/multiplayer evidence. */
public final class KitchenTests {
    static class ControlledPlayer extends ServerPlayer {
        Float roll;
        float[] script;int draw;
        final RandomSource controlled=new LegacyRandomSource(12){@Override public float nextFloat(){return script!=null&&draw<script.length?script[draw++]:roll==null?super.nextFloat():roll;}};
        ControlledPlayer(GameTestHelper h,CommonListenerCookie c){super(h.getLevel().getServer(),h.getLevel(),c.gameProfile(),c.clientInformation());}
        @Override public RandomSource getRandom(){return controlled==null?super.getRandom():controlled;}
    }
    static ControlledPlayer player(GameTestHelper h,BlockPos pos){
        var c=CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),"KitchenTest"),false);
        var p=new ControlledPlayer(h,c);var connection=new Connection(PacketFlow.SERVERBOUND);new EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,c);p.setGameMode(GameType.SURVIVAL);p.connection.markClientLoaded();
        p.snapTo(pos.getX()+.5,pos.getY(),pos.getZ()+2,180,20);
        h.getLevel().getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION,false,h.getLevel().getServer());return p;
    }
    static BlockHitResult hit(BlockPos p){return new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false);}
    static void use(GameTestHelper h,BlockPos pos,ServerPlayer p){
        h.getLevel().getBlockState(pos).useItemOn(p.getMainHandItem(),h.getLevel(),p,InteractionHand.MAIN_HAND,hit(pos));
    }
    static int drops(GameTestHelper h,BlockPos p,Item i){return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(p).inflate(2)).stream().filter(e->e.getItem().is(i)).mapToInt(e->e.getItem().getCount()).sum();}
    static void clearDrops(GameTestHelper h,BlockPos p){h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(p).inflate(2)).forEach(ItemEntity::discard);}
    static CraftingInput grid(int w,int height,Item... items){return CraftingInput.of(w,height,Arrays.stream(items).map(i->i==Items.AIR?ItemStack.EMPTY:new ItemStack(i)).toList());}
    static ItemStack craft(GameTestHelper h,CraftingInput input){return h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).map(r->r.value().assemble(input)).orElse(ItemStack.EMPTY);}
    public static void recipes(GameTestHelper h){
        for(int size:new int[]{2,3})for(int y=0;y<size-1;y++)for(int x=0;x<size;x++){
            Item[] grid=new Item[size*size];Arrays.fill(grid,Items.AIR);grid[y*size+x]=Items.IRON_INGOT;grid[(y+1)*size+x]=Items.STICK;
            h.assertTrue(craft(h,grid(size,size,grid)).is(UnityFeastMod.KITCHEN_KNIFE),"knife translated in 2x2/3x3");
        }
        for(var wrong:List.of(new Item[]{Items.STICK,Items.IRON_INGOT},new Item[]{Items.IRON_INGOT,Items.STICK,Items.AIR,Items.AIR},new Item[]{Items.IRON_INGOT,Items.AIR,Items.AIR,Items.STICK}))
            h.assertTrue(!craft(h,grid(wrong.length==2?1:2,2,wrong)).is(UnityFeastMod.KITCHEN_KNIFE),"no reversed/horizontal/diagonal knife");
        for(var wood:List.of(Items.OAK_PLANKS,Items.BIRCH_PLANKS,Items.BAMBOO_PLANKS,Items.CRIMSON_PLANKS))for(int y=0;y<3;y++){
            Item[] g=new Item[9];Arrays.fill(g,Items.AIR);g[y*3]=wood;g[y*3+1]=wood;g[y*3+2]=Items.STICK;
            h.assertTrue(craft(h,grid(3,3,g)).is(UnityFeastMod.CUTTING_BOARD_ITEM),"same plank board all rows");
        }
        h.assertTrue(craft(h,grid(3,1,Items.OAK_PLANKS,Items.BIRCH_PLANKS,Items.STICK)).isEmpty(),"mixed planks rejected");
        h.assertTrue(craft(h,grid(3,1,Items.STICK,Items.OAK_PLANKS,Items.OAK_PLANKS)).isEmpty(),"reversed board rejected");
        h.assertTrue(craft(h,grid(3,3,Items.BRICK,Items.AIR,Items.BRICK,Items.BRICK,Items.AIR,Items.BRICK,Items.BRICK,Items.BRICK,Items.BRICK)).is(UnityFeastMod.SAUCE_VAT_ITEM),"seven brick items make vat");
        for(int row=0;row<2;row++){
            Item[] g=new Item[9];Arrays.fill(g,Items.AIR);for(int x=0;x<3;x++)g[row*3+x]=UnityFeastMod.GREENBELT_ITEM.get();g[(row+1)*3+1]=UnityFeastMod.SOY_PASTE.get();
            var input=grid(3,3,g);var recipe=h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow().value();
            var out=recipe.assemble(input);h.assertTrue(out.is(UnityFeastMod.DIPPED_LETTUCE)&&out.getCount()==3,"translated dipped lettuce produces three");
            var rest=recipe.getRemainingItems(input);h.assertValueEqual(rest.stream().filter(s->s.is(Items.BOWL)).mapToInt(ItemStack::getCount).sum(),1,"one craft bowl only");
        }
        System.out.println("V120 PASS recipes: knife 8 translations, board identities/order, 7 bricks, lettuce and bowl remainder");h.succeed();
    }
    public static void vat(GameTestHelper h){
        var l=h.getLevel();var pos=h.absolutePos(new BlockPos(1,2,1));var p=player(h,pos);var q=player(h,pos);
        l.setBlock(pos,UnityFeastMod.SAUCE_VAT.get().defaultBlockState(),3);var cocoa=new ItemStack(Items.COCOA_BEANS,4);p.setItemInHand(InteractionHand.MAIN_HAND,cocoa);
        for(int i=1;i<=3;i++){use(h,pos,p);h.assertValueEqual(l.getBlockState(pos).getValue(SauceVatBlock.FILL),i,"one cocoa per level");}
        use(h,pos,p);h.assertValueEqual(cocoa.getCount(),1,"fourth cocoa retained");
        var full=l.getBlockState(pos);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOWL));q.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOWL));
        use(h,pos,p);full.useItemOn(q.getMainHandItem(),l,q,InteractionHand.MAIN_HAND,hit(pos));
        h.assertTrue(p.getMainHandItem().is(UnityFeastMod.SOY_PASTE)&&q.getMainHandItem().is(Items.BOWL),"two server players using stale full state produce only one paste");
        h.assertValueEqual(l.getBlockState(pos).getValue(SauceVatBlock.FILL),0,"one bowl empties vat");
        for(int fill=0;fill<3;fill++){
            l.setBlock(pos,full.setValue(SauceVatBlock.FILL,fill),3);use(h,pos,q);h.assertTrue(q.getMainHandItem().is(Items.BOWL),"incomplete vats do not consume bowl");
            h.assertValueEqual(Block.getDrops(l.getBlockState(pos),l,pos,null).getFirst().getCount(),1,"break drops empty vat one");
        }
        for(int slot=0;slot<p.getInventory().getContainerSize();slot++)p.getInventory().setItem(slot,new ItemStack(Items.STONE,64));
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOWL,2));l.setBlock(pos,full,3);clearDrops(h,pos);use(h,pos,p);
        h.assertValueEqual(p.getMainHandItem().getCount(),1,"stacked bowls consume only one");h.assertValueEqual(drops(h,pos,UnityFeastMod.SOY_PASTE.get()),1,"full inventory drops one paste");
        p.setGameMode(GameType.CREATIVE);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COCOA_BEANS,3));
        for(int i=0;i<3;i++)use(h,pos,p);h.assertValueEqual(p.getMainHandItem().getCount(),3,"creative cocoa not consumed");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOWL,4));use(h,pos,p);h.assertValueEqual(p.getMainHandItem().getCount(),4,"creative bowl not consumed");
        h.assertValueEqual(l.getBlockState(pos).getValue(SauceVatBlock.FILL),0,"creative extraction resets vat");
        System.out.println("V120 PASS vat fill, full/partial, two-player serialized race, full inventory and creative conservation");h.succeed();
    }
    public static void board(GameTestHelper h){
        var l=h.getLevel();var pos=h.absolutePos(new BlockPos(1,2,1));var p=player(h,pos);var q=player(h,pos);
        l.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);l.setBlock(pos,UnityFeastMod.CUTTING_BOARD.get().defaultBlockState(),3);
        var be=(CuttingBoardBlockEntity)l.getBlockEntity(pos);
        for(var raw:List.of(Items.PORKCHOP,Items.SALMON,Items.CHICKEN)){
            var stack=new ItemStack(raw,2);stack.set(DataComponents.CUSTOM_NAME,Component.literal("persisted ingredient"));p.setItemInHand(InteractionHand.MAIN_HAND,stack);use(h,pos,p);use(h,pos,p);
            h.assertValueEqual(stack.getCount(),1,"occupied board preserves second input");h.assertTrue(be.ingredient().is(raw)&&be.ingredient().getCount()==1,"one raw slot");
            var saved=be.saveCustomOnly(l.registryAccess());var loaded=new CuttingBoardBlockEntity(pos,be.getBlockState());loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),saved));
            h.assertTrue(loaded.ingredient().is(raw)&&loaded.ingredient().has(DataComponents.CUSTOM_NAME),"disk codec preserves ingredient components");
            var loot=Block.getDrops(be.getBlockState(),l,pos,loaded);h.assertTrue(loot.size()==2&&loot.stream().anyMatch(s->s.is(raw)),"break contains board plus original ingredient");
            var tool=new ItemStack(UnityFeastMod.KITCHEN_KNIFE.get());tool.setDamageValue(250);p.setItemInHand(InteractionHand.MAIN_HAND,tool);q.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(UnityFeastMod.KITCHEN_KNIFE.get()));
            clearDrops(h,pos);use(h,pos,p);use(h,pos,q);var output=CuttingBoardBlock.resultFor(new ItemStack(raw));
            h.assertValueEqual(drops(h,pos,output.getItem()),output.getCount(),"one ingredient yields exact output despite two users");
            h.assertTrue(be.ingredient().isEmpty()&&tool.isEmpty(),"last durability completes output before breaking");h.assertValueEqual(q.getMainHandItem().getDamageValue(),0,"empty board does not damage second knife");
        }
        for(var invalid:List.of(Items.COOKED_PORKCHOP,Items.COD,Items.COOKED_CHICKEN,UnityFeastMod.STINKY_FISH.get(),UnityFeastMod.RAW_PORK_SLICES.get())){
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(invalid));use(h,pos,p);h.assertTrue(be.ingredient().isEmpty(),"invalid ingredient rejected");
        }
        var completeKnife=new ItemStack(UnityFeastMod.KITCHEN_KNIFE.get());p.setItemInHand(InteractionHand.MAIN_HAND,completeKnife);clearDrops(h,pos);
        for(int i=0;i<251;i++){
            be.setIngredient(new ItemStack(Items.PORKCHOP));use(h,pos,p);
            h.assertTrue(be.ingredient().isEmpty(),"every one of 251 legal operations completes");
            if(i<250)h.assertTrue(!completeKnife.isEmpty()&&completeKnife.getDamageValue()==i+1,"no double durability or early break");
        }
        h.assertTrue(completeKnife.isEmpty(),"knife breaks exactly on operation 251");h.assertValueEqual(drops(h,pos,UnityFeastMod.RAW_PORK_SLICES.get()),251,"251 ingredients yield 251 products");
        be.setIngredient(new ItemStack(Items.SALMON));p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));use(h,pos,p);h.assertTrue(be.ingredient().is(Items.SALMON),"ordinary sword cannot process");
        clearDrops(h,pos);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);l.getBlockState(pos).useWithoutItem(l,p,hit(pos));h.assertValueEqual(drops(h,pos,Items.SALMON),1,"empty hand retrieves original");
        clearDrops(h,pos);be.setIngredient(new ItemStack(Items.CHICKEN));l.destroyBlock(pos.below(),false);h.assertTrue(l.getBlockState(pos).isAir(),"unsupported board removed");
        h.assertValueEqual(drops(h,pos,UnityFeastMod.CUTTING_BOARD_ITEM.get()),1,"support loss drops board once");h.assertValueEqual(drops(h,pos,Items.CHICKEN),1,"support loss drops raw once");
        var knife=new ItemStack(UnityFeastMod.KITCHEN_KNIFE.get());h.assertValueEqual(knife.getMaxDamage(),251,"knife durability 251");
        for(var e:l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().toList())h.assertTrue(!knife.supportsEnchantment(e)&&!knife.isPrimaryItemFor(e),"all registered enchantments rejected");
        var book=new ItemStack(Items.ENCHANTED_BOOK);book.enchant(l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS),1);
        for(var mode:List.of(GameType.SURVIVAL,GameType.CREATIVE)){
            p.setGameMode(mode);var anvil=new AnvilMenu(5,p.getInventory());anvil.getSlot(0).set(knife.copy());anvil.getSlot(1).set(book);anvil.createResult();h.assertTrue(anvil.getSlot(2).getItem().isEmpty(),"survival and creative anvil reject enchanted books");
            anvil.getSlot(1).set(ItemStack.EMPTY);anvil.setItemName("My Kitchen Knife");h.assertTrue(!anvil.getSlot(2).getItem().isEmpty(),"ordinary rename remains available");
        }
        p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.setItemInHand(InteractionHand.MAIN_HAND,knife);p.doTick();h.assertValueEqual(p.getAttributeValue(Attributes.ATTACK_DAMAGE),4D,"final knife damage");h.assertValueEqual(p.getAttributeValue(Attributes.ATTACK_SPEED),1D,"final knife attack speed");
        var cow=EntityType.COW.create(l,EntitySpawnReason.COMMAND);cow.snapTo(p.getX(),p.getY(),p.getZ()+1,0,0);l.addFreshEntity(cow);p.setOnGround(true);p.setDeltaMovement(Vec3.ZERO);
        for(int i=0;i<25;i++)p.doTick();p.setOnGround(true);p.fallDistance=0;float before=cow.getHealth();p.attack(cow);
        h.assertValueEqual(before-cow.getHealth(),4F,"full cooldown noncritical attack deals four");h.assertValueEqual(knife.getDamageValue(),1,"effective attack costs one durability");cow.discard();
        System.out.println("V120 PASS board atomic operations, component save/load, all 251 operations and last-use output, support loot; knife attributes/enchantments/actual attack");h.succeed();
    }
    public static void foods(GameTestHelper h){
        var l=h.getLevel();var pos=h.absolutePos(new BlockPos(1,2,1));var p=player(h,pos);
        Item[] items={UnityFeastMod.GREENBELT_ITEM.get(),UnityFeastMod.SOY_PASTE.get(),UnityFeastMod.DIPPED_LETTUCE.get(),UnityFeastMod.RAW_PORK_SLICES.get(),UnityFeastMod.SALMON_SASHIMI.get(),UnityFeastMod.WHITE_CUT_CHICKEN.get()};
        int[] nutrition={2,6,4,4,2,6};
        for(int i=0;i<items.length;i++){
            p.removeAllEffects();p.roll=.99F;p.getFoodData().setFoodLevel(0);p.getFoodData().setSaturation(0);
            var food=new ItemStack(items[i]);h.assertValueEqual(food.get(DataComponents.CONSUMABLE).consumeTicks(),32,"32 tick eating");
            var result=food.finishUsingItem(l,p);h.assertValueEqual(p.getFoodData().getFoodLevel(),nutrition[i],"actual hunger restoration");
            h.assertTrue(i==1?result.is(Items.BOWL):result.isEmpty(),"exact eating remainder");
            if(i==1||i==3)h.assertValueEqual(p.getEffect(MobEffects.NAUSEA).getDuration(),100,"guaranteed nausea");
            if(i==2){h.assertValueEqual(p.getEffect(MobEffects.STRENGTH).getAmplifier(),1,"strength II");h.assertValueEqual(p.getEffect(MobEffects.STRENGTH).getDuration(),200,"strength ten seconds");h.assertTrue(!p.hasEffect(MobEffects.NAUSEA),"no ingredient effects");}
            if(i==5)h.assertValueEqual(p.getEffect(MobEffects.LEVITATION).getDuration(),200,"levitation ten seconds");
            p.getFoodData().setFoodLevel(20);h.assertTrue(!new ItemStack(items[i]).get(DataComponents.CONSUMABLE).canConsume(p,new ItemStack(items[i])),"full food disallowed");
        }
        for(var item:items)for(var e:new ItemStack(item).get(DataComponents.CONSUMABLE).onConsumeEffects()){
            var effect=(ApplyStatusEffectsConsumeEffect)e;float threshold=effect.probability();if(threshold==1)continue;
            for(float value:new float[]{Math.nextDown(threshold),threshold,Math.nextUp(threshold)}){
                p.removeAllEffects();p.roll=value;boolean applied=effect.apply(l,new ItemStack(item),p);h.assertTrue(applied==(value<threshold),"probability boundary below/equal/above "+threshold);
                if(applied)h.assertValueEqual(p.getEffect(effect.effects().getFirst().getEffect()).getDuration(),effect.effects().getFirst().getDuration(),"probabilistic duration");
            }
        }
        p.roll=.05F;l.getServer().setDifficulty(Difficulty.NORMAL,true);l.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);p.snapTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        h.assertTrue(GreenbeltItem.spawnSilverfish(l,p),"silverfish successfully spawns at unobstructed feet without natural distance restriction");
        var area=new AABB(pos).inflate(3);l.getEntitiesOfClass(net.minecraft.world.entity.monster.Silverfish.class,area).forEach(Entity::discard);
        for(float nausea:new float[]{.29F,.3F})for(float fish:new float[]{Math.nextDown(.1F),.1F,Math.nextUp(.1F)}){
            p.script=new float[]{.5F,.5F,.5F,.5F,.5F,.5F,nausea,fish};p.draw=0;p.removeAllEffects();p.getFoodData().setFoodLevel(0);
            new ItemStack(UnityFeastMod.GREENBELT_ITEM.get()).finishUsingItem(l,p);
            var spawned=l.getEntitiesOfClass(net.minecraft.world.entity.monster.Silverfish.class,area);
            h.assertTrue(p.hasEffect(MobEffects.NAUSEA)==(nausea<.3F),"independent nausea boundary");h.assertValueEqual(spawned.size(),fish<.1F?1:0,"independent silverfish below/equal/above ten percent");spawned.forEach(Entity::discard);
        }
        p.script=null;p.roll=null;p.getRandom().setSeed(120);int nauseas=0,silverfish=0,both=0;
        for(int i=0;i<1000;i++){
            p.removeAllEffects();p.getFoodData().setFoodLevel(0);new ItemStack(UnityFeastMod.GREENBELT_ITEM.get()).finishUsingItem(l,p);
            var spawned=l.getEntitiesOfClass(net.minecraft.world.entity.monster.Silverfish.class,area);boolean nausea=p.hasEffect(MobEffects.NAUSEA);if(nausea)nauseas++;silverfish+=spawned.size();if(nausea&&!spawned.isEmpty())both++;spawned.forEach(Entity::discard);
        }
        h.assertTrue(nauseas>250&&nauseas<350&&silverfish>70&&silverfish<130&&both>0,"1000 completed foods produce independent real events near thirty/ten percent");
        System.out.println("V120 REAL CONSUMPTION samples=1000 nausea="+nauseas+" silverfish="+silverfish+" both="+both);
        p.roll=.05F;
        l.getServer().setDifficulty(Difficulty.PEACEFUL,true);
        for(int i=0;i<40;i++){p.getFoodData().setFoodLevel(0);new ItemStack(UnityFeastMod.GREENBELT_ITEM.get()).finishUsingItem(l,p);}
        h.assertTrue(l.getEntitiesOfClass(net.minecraft.world.entity.monster.Silverfish.class,area).isEmpty()&&p.hasEffect(MobEffects.NAUSEA),"peaceful never spawns but nausea still applies");
        p.setGameMode(GameType.CREATIVE);p.getFoodData().setFoodLevel(0);var sauce=new ItemStack(UnityFeastMod.SOY_PASTE.get());h.assertTrue(sauce.finishUsingItem(l,p).is(UnityFeastMod.SOY_PASTE),"creative consumption cannot duplicate bowls");
        p.setGameMode(GameType.SURVIVAL);p.roll=null;p.getFoodData().setFoodLevel(0);p.removeAllEffects();var uneaten=new ItemStack(UnityFeastMod.SOY_PASTE.get());p.setItemInHand(InteractionHand.OFF_HAND,uneaten);uneaten.use(l,p,InteractionHand.OFF_HAND);p.stopUsingItem();
        h.assertTrue(uneaten.getCount()==1&&p.getFoodData().getFoodLevel()==0&&!p.hasEffect(MobEffects.NAUSEA),"cancelled eating no consume/effects/remainder");
        p.setNoGravity(true);for(int i=0;i<p.getInventory().getContainerSize();i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(UnityFeastMod.SOY_PASTE.get()));
        p.getOffhandItem().use(l,p,InteractionHand.OFF_HAND);for(int i=0;i<31;i++)p.doTick();h.assertValueEqual(p.getFoodData().getFoodLevel(),0,"no food before tick 32");p.doTick();
        h.assertTrue(p.getOffhandItem().is(Items.BOWL)&&p.getFoodData().getFoodLevel()==6,"actual full-duration offhand food returns one bowl with inventory full");
        l.getServer().setDifficulty(Difficulty.NORMAL,true);System.out.println("V120 PASS nutrition/effects/consume remainders, exact probability boundaries, peaceful silverfish exclusion and cancellation");h.succeed();
    }
    public static void plant(GameTestHelper h){
        var l=h.getLevel();var pos=h.absolutePos(new BlockPos(1,2,1));var p=player(h,pos);var crop=UnityFeastMod.GREENBELT.get();
        for(var soil:List.of(Blocks.GRASS_BLOCK,Blocks.DIRT,Blocks.FARMLAND)){
            l.setBlock(pos,Blocks.AIR.defaultBlockState(),3);l.setBlock(pos.below(),soil.defaultBlockState(),3);var stack=new ItemStack(UnityFeastMod.GREENBELT_ITEM.get(),2);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
            h.assertTrue(stack.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,hit(pos.below()))).consumesAction(),"planting succeeds on specified soil");
            h.assertTrue(l.getBlockState(pos).is(crop)&&l.getBlockState(pos).getValue(CropBlock.AGE)==0&&stack.getCount()==1&&!p.isUsingItem(),"plant begins at zero with one consumed and no eating");
            stack.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,hit(pos)));h.assertValueEqual(stack.getCount(),1,"cannot overwrite own plant");
            h.assertValueEqual(Block.getDrops(l.getBlockState(pos),l,pos,null).getFirst().getCount(),1,"immature no multiplication");
        }
        for(int age=0;age<8;age++){
            var state=crop.getStateForAge(age);l.setBlock(pos,state,3);int n=Block.getDrops(state,l,pos,null).getFirst().getCount();h.assertTrue(age==7?n==2||n==3:n==1,"age-dependent loot");
        }
        int twos=0,threes=0;for(int i=0;i<2000;i++){int n=Block.getDrops(crop.getStateForAge(7),l,pos,null,p,new ItemStack(Items.SHEARS)).getFirst().getCount();if(n==2)twos++;else if(n==3)threes++;else h.fail("bad mature count");}
        h.assertTrue(twos>900&&twos<1100&&twos+threes==2000,"mature 50/50 statistical smoke");
        l.setBlock(pos,crop.getStateForAge(0),3);crop.performBonemeal(l,l.getRandom(),pos,l.getBlockState(pos));int age=l.getBlockState(pos).getValue(CropBlock.AGE);h.assertTrue(age>=2&&age<=5,"potato bone meal progresses 2..5");
        l.setBlock(pos,crop.getStateForAge(7),3);h.assertTrue(!crop.isValidBonemealTarget(l,pos,l.getBlockState(pos)),"mature cannot become tall grass");
        // Stable soil/layout equivalence to the vanilla dry/wet crop coefficients.
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){l.setBlock(pos.offset(x,0,z),Blocks.AIR.defaultBlockState(),2);l.setBlock(pos.below().offset(x,0,z),Blocks.DIRT.defaultBlockState(),2);}
        l.setBlock(pos,crop.getStateForAge(0),2);l.setBlock(pos.below().offset(0,0,3),Blocks.STONE.defaultBlockState(),2);
        h.assertValueEqual(crop.growthSpeed(l,pos),4F,"dry 3x3 equivalent farmland coefficient");
        l.setBlock(pos.below().offset(0,0,3),Blocks.WATER.defaultBlockState(),2);h.assertValueEqual(crop.growthSpeed(l,pos),10F,"wet 3x3 equivalent farmland coefficient");
        l.setBlock(pos.east(),crop.getStateForAge(0),2);l.setBlock(pos.north(),crop.getStateForAge(0),2);h.assertValueEqual(crop.growthSpeed(l,pos),5F,"dense layout halves growth like potato");
        l.setBlock(pos.below().offset(0,0,3),Blocks.STONE.defaultBlockState(),2);
        System.out.println("V120 PASS plant supports, ages 0..7, drops 2="+twos+" 3="+threes+", bone meal and dry/wet/layout speed coefficients");h.succeed();
    }
    public static void generation(GameTestHelper h){
        for(long seed:new long[]{120,2026,987654321}){
            int green=0;for(int i=0;i<10000;i++){
                var p=new BlockPos(i%100,64+(i%7),i/100);var original=Blocks.SHORT_GRASS.defaultBlockState();var result=GreenbeltGeneration.replace(original,seed,p);
                h.assertTrue(result.equals(GreenbeltGeneration.replace(original,seed,p)),"seed deterministic");if(result.is(UnityFeastMod.GREENBELT)){green++;h.assertValueEqual(result.getValue(CropBlock.AGE),7,"natural mature");}
                for(var unchanged:List.of(Blocks.FERN,Blocks.TALL_GRASS,Blocks.DANDELION))h.assertTrue(GreenbeltGeneration.replace(unchanged.defaultBlockState(),seed,p).is(unchanged),"other vegetation unchanged");
            }
            h.assertTrue(green>=1800&&green<=2200,"twenty percent denominator is total candidates");System.out.println("V120 DETERMINISTIC CANDIDATES seed="+seed+" original=10000 replaced="+green+" short_grass="+(10000-green));
        }
        h.succeed();
    }

    public static void craftingContainers(GameTestHelper h){
        var l=h.getLevel();var pos=h.absolutePos(new BlockPos(1,2,1));var p=player(h,pos);
        l.setBlock(pos,Blocks.CRAFTING_TABLE.defaultBlockState(),3);
        var menu=new CraftingMenu(12,p.getInventory(),ContainerLevelAccess.create(l,pos));p.containerMenu=menu;
        for(int slot=1;slot<=3;slot++)menu.getSlot(slot).set(new ItemStack(UnityFeastMod.GREENBELT_ITEM.get(),3));
        menu.getSlot(5).set(new ItemStack(UnityFeastMod.SOY_PASTE.get()));
        h.assertTrue(menu.getSlot(0).getItem().is(UnityFeastMod.DIPPED_LETTUCE),"real workbench preview");
        menu.quickMoveStack(p,0);
        h.assertValueEqual(p.getInventory().countItem(UnityFeastMod.DIPPED_LETTUCE.get()),3,"shift output exactly three");
        h.assertTrue(menu.getSlot(5).getItem().is(Items.BOWL)&&menu.getSlot(5).getItem().getCount()==1,"shift returns one bowl to consumed slot");
        for(int slot=1;slot<=3;slot++)h.assertValueEqual(menu.getSlot(slot).getItem().getCount(),2,"shift consumes one leaf per slot");
        h.assertTrue(menu.getSlot(0).getItem().isEmpty(),"returned bowl cannot act as paste");
        for(int i=1;i<=9;i++)menu.getSlot(i).set(ItemStack.EMPTY);
        p.getInventory().clearContent();p.getInventory().add(new ItemStack(UnityFeastMod.GREENBELT_ITEM.get(),6));p.getInventory().add(new ItemStack(UnityFeastMod.SOY_PASTE.get()));
        var input=grid(3,2,UnityFeastMod.GREENBELT_ITEM.get(),UnityFeastMod.GREENBELT_ITEM.get(),UnityFeastMod.GREENBELT_ITEM.get(),Items.AIR,UnityFeastMod.SOY_PASTE.get(),Items.AIR);
        var recipe=l.recipeAccess().getRecipeFor(RecipeType.CRAFTING,input,l).orElseThrow();
        menu.handlePlacement(false,false,recipe,l,p.getInventory());h.assertTrue(menu.getSlot(0).getItem().is(UnityFeastMod.DIPPED_LETTUCE),"recipe book places valid pattern");menu.quickMoveStack(p,0);
        h.assertValueEqual(p.getInventory().countItem(UnityFeastMod.DIPPED_LETTUCE.get()),3,"recipe-book shift crafted exact result");
        p.containerMenu=p.inventoryMenu;
        var crafter=pos.east(2);l.setBlock(crafter,Blocks.CRAFTER.defaultBlockState(),3);var be=(net.minecraft.world.level.block.entity.CrafterBlockEntity)l.getBlockEntity(crafter);
        for(int i=0;i<3;i++)be.setItem(i,new ItemStack(UnityFeastMod.GREENBELT_ITEM.get()));be.setItem(4,new ItemStack(UnityFeastMod.SOY_PASTE.get()));
        clearDrops(h,crafter);l.getBlockState(crafter).tick(l,crafter,l.getRandom());
        h.assertValueEqual(drops(h,crafter,UnityFeastMod.DIPPED_LETTUCE.get()),3,"vanilla crafter dispenses three lettuce");h.assertValueEqual(drops(h,crafter,Items.BOWL),1,"vanilla crafter dispenses one bowl");
        System.out.println("V120 PASS real CraftingMenu shift/recipe-book leaf, paste and bowl conservation");h.succeed();
    }

    public static void growth(GameTestHelper h){
        var l=h.getLevel();var a=h.absolutePos(new BlockPos(1,2,1));var b=a.east(4);var lettuce=UnityFeastMod.GREENBELT.get();
        // Do not let the global random tick scheduler add unrelated draws to this paired comparison.
        int prior=l.getGameRules().get(GameRules.RANDOM_TICK_SPEED);l.getGameRules().set(GameRules.RANDOM_TICK_SPEED,0,l.getServer());
        for(var center:List.of(a,b)){
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){l.setBlock(center.offset(x,0,z),Blocks.AIR.defaultBlockState(),2);l.setBlock(center.below().offset(x,0,z),Blocks.STONE.defaultBlockState(),2);}
            l.setBlock(center.above(2),Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL,15),2);
        }
        h.succeedWhen(()->{
            h.assertTrue(l.getRawBrightness(a,0)>=9&&l.getRawBrightness(b,0)>=9&&l.isAreaLoaded(a,5)&&l.isAreaLoaded(b,5),"paired crop illumination and loaded neighbors ready");
            for(boolean wet:List.of(false,true))for(boolean dense:List.of(false,true)){
                for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
                    l.setBlock(a.below().offset(x,0,z),Blocks.DIRT.defaultBlockState(),2);
                    l.setBlock(b.below().offset(x,0,z),Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,wet?7:0),2);
                    l.setBlock(a.offset(x,0,z),dense?lettuce.getStateForAge(0):Blocks.AIR.defaultBlockState(),2);
                    l.setBlock(b.offset(x,0,z),dense?Blocks.POTATOES.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
                }
                l.setBlock(a.below().south(3),wet?Blocks.WATER.defaultBlockState():Blocks.STONE.defaultBlockState(),2);
                int total=0;
                for(int seed=0;seed<64;seed++){
                    l.setBlock(a,lettuce.getStateForAge(0),2);l.setBlock(b,Blocks.POTATOES.defaultBlockState(),2);
                    var ra=RandomSource.create(seed);var rb=RandomSource.create(seed);int ticks=0;
                    while(l.getBlockState(a).getValue(CropBlock.AGE)<7&&ticks++<1000){
                        l.getBlockState(a).randomTick(l,a,ra);l.getBlockState(b).randomTick(l,b,rb);
                        h.assertValueEqual(l.getBlockState(a).getValue(CropBlock.AGE),l.getBlockState(b).getValue(CropBlock.AGE),"potato/lettuce identical seeded age progression wet="+wet+" dense="+dense);
                    }
                    h.assertTrue(ticks<1000,"both mature in bounded random ticks");total+=ticks;
                }
                System.out.println("V120 GROWTH paired vanilla randomTick seed pairs=64 wet="+wet+" dense="+dense+" mean-random-ticks="+total/64.0);
            }
            l.setBlock(a.below().south(3),Blocks.STONE.defaultBlockState(),2);
            l.getGameRules().set(GameRules.RANDOM_TICK_SPEED,prior,l.getServer());
        });
    }
}
