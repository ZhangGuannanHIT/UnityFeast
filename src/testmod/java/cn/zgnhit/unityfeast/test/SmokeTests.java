package cn.zgnhit.unityfeast.test;

import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.player.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.gamerules.GameRules;

/** Runs the real recipe, furnace, consumption, damage and player persistence paths. */
public final class SmokeTests {
    static void effect(GameTestHelper h,ServerPlayer p,Holder<MobEffect> effect,int ticks,int amp){
        var e=p.getEffect(effect);h.assertTrue(e!=null&&e.getDuration()==ticks&&e.getAmplifier()==amp,"exact effect "+effect+" "+ticks+" amp "+amp);
    }
    public static void recipes(GameTestHelper h){
        var cap=UnityFeastMod.BOTTLE_CAP.get();var tobacco=UnityFeastMod.TOBACCO.get();var cigarette=UnityFeastMod.CIGARETTE.get();var a=Items.AIR;
        h.assertTrue(KitchenTests.craft(h,KitchenTests.grid(3,3,a,Items.PAPER,a,tobacco,tobacco,tobacco,a,Items.PAPER,a)).is(cigarette),"two paper three tobacco");
        h.assertTrue(KitchenTests.craft(h,KitchenTests.grid(3,3,a,Items.PAPER,a,tobacco,tobacco,tobacco,a,a,a)).isEmpty(),"missing bottom paper rejected");
        h.assertTrue(KitchenTests.craft(h,KitchenTests.grid(3,3,a,cap,a,cap,cigarette,cap,a,cap,a)).is(UnityFeastMod.XINGQING),"four caps and cigarette make xingqing");
        var parts=List.of(UnityFeastMod.SILVERWING_HELMET.get(),UnityFeastMod.SILVERWING_CHESTPLATE.get(),UnityFeastMod.SILVERWING_LEGGINGS.get(),UnityFeastMod.SILVERWING_BOOTS.get());
        int[][] indices={{0,1,2,3,5},{0,2,3,4,5,6,7,8},{0,1,2,3,5,6,8},{0,2,3,5}};
        for(int i=0;i<4;i++)for(int shift=0;shift<(i==0||i==3?2:1);shift++){
            Item[] grid=new Item[9];Arrays.fill(grid,a);for(int slot:indices[i])grid[slot+shift*3]=cap;
            var input=KitchenTests.grid(3,3,grid);var result=KitchenTests.craft(h,input);
            h.assertTrue(result.is(parts.get(i))&&result.getCount()==1,"armor material count and legal vertical translations "+i);
            grid[indices[i][0]+shift*3]=Items.IRON_NUGGET;
            h.assertTrue(KitchenTests.craft(h,KitchenTests.grid(3,3,grid)).isEmpty(),"other metal is not cap");
        }
        var m=h.getLevel().recipeAccess();
        var t=m.getRecipeFor(RecipeType.SMELTING,new SingleRecipeInput(new ItemStack(Items.WHEAT)),h.getLevel()).orElseThrow().value();
        var c=m.getRecipeFor(RecipeType.SMELTING,new SingleRecipeInput(new ItemStack(Items.OAK_LOG)),h.getLevel()).orElseThrow().value();
        h.assertTrue(t.assemble(new SingleRecipeInput(new ItemStack(Items.WHEAT))).is(tobacco)&&t.cookingTime()==c.cookingTime()&&t.experience()==c.experience(),"tobacco charcoal 200 ticks and .15 experience");
        h.assertTrue(m.getRecipeFor(RecipeType.SMOKING,new SingleRecipeInput(new ItemStack(Items.WHEAT)),h.getLevel()).isEmpty(),"no smoker shortcut");
        h.assertTrue(m.getRecipeFor(RecipeType.SMELTING,new SingleRecipeInput(new ItemStack(Items.WHEAT_SEEDS)),h.getLevel()).isEmpty(),"no seeds shortcut");
        var p1=h.absolutePos(new BlockPos(1,2,1));var p2=p1.east(3);
        h.getLevel().setBlock(p1,Blocks.FURNACE.defaultBlockState(),3);h.getLevel().setBlock(p2,Blocks.FURNACE.defaultBlockState(),3);
        var f1=(FurnaceBlockEntity)h.getLevel().getBlockEntity(p1);var f2=(FurnaceBlockEntity)h.getLevel().getBlockEntity(p2);
        f1.setItem(0,new ItemStack(Items.WHEAT,2));f2.setItem(0,new ItemStack(Items.OAK_LOG,2));
        f1.setItem(1,new ItemStack(Items.COAL));f2.setItem(1,new ItemStack(Items.COAL));
        for(int tick=1;tick<=400;tick++){
            AbstractFurnaceBlockEntity.serverTick(h.getLevel(),p1,h.getLevel().getBlockState(p1),f1);
            AbstractFurnaceBlockEntity.serverTick(h.getLevel(),p2,h.getLevel().getBlockState(p2),f2);
            var n1=f1.saveCustomOnly(h.getLevel().registryAccess());var n2=f2.saveCustomOnly(h.getLevel().registryAccess());
            for(var key:List.of("lit_time_remaining","lit_total_time","cooking_time_spent","cooking_total_time"))
                h.assertTrue(n1.getIntOr(key,-999)==n2.getIntOr(key,-998),"real furnaces same fuel/progress each tick "+tick+" "+key);
            h.assertTrue(f1.getItem(2).getCount()==f2.getItem(2).getCount(),"same output cadence");
        }
        h.assertTrue(f1.getItem(2).is(tobacco)&&f1.getItem(2).getCount()==2&&f2.getItem(2).is(Items.CHARCOAL),"two input two output after400 ticks");
        System.out.println("V130 PASS recipes: armor5/8/7/4, paper2+tobacco3, cap4+cigarette; paired real furnaces400 ticks fuel and cooking match charcoal");h.succeed();
    }
    public static void foods(GameTestHelper h){
        var l=h.getLevel();var p=KitchenTests.player(h,h.absolutePos(BlockPos.ZERO));
        p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(7);p.setHealth(20);
        for(var item:List.of(UnityFeastMod.TOBACCO.get(),UnityFeastMod.CIGARETTE.get())){
            p.removeAllEffects();var stack=new ItemStack(item,3);p.setItemSlot(EquipmentSlot.MAINHAND,stack);
            h.assertTrue(!stack.has(DataComponents.FOOD)&&stack.get(DataComponents.CONSUMABLE).consumeTicks()==32,"non nutritive32ticks");
            h.assertTrue(item.use(l,p,InteractionHand.MAIN_HAND).consumesAction(),"usable at full hunger");
            p.stopUsingItem();h.assertTrue(stack.getCount()==3&&!p.hasEffect(MobEffects.POISON)&&!p.hasEffect(MobEffects.SPEED)&&p.getHealth()==20,"cancel has no consumption or effect");
        }
        p.removeAllEffects();var t=new ItemStack(UnityFeastMod.TOBACCO.get(),2);t.finishUsingItem(l,p);
        h.assertTrue(t.getCount()==1&&p.getHealth()==20&&p.getFoodData().getFoodLevel()==20&&p.getFoodData().getSaturationLevel()==7,"tobacco consumes1 no nutrition no instantdamage");effect(h,p,MobEffects.POISON,100,0);
        p.removeAllEffects();p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,2000,4));p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,2000,1));
        p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.NETHERITE_CHESTPLATE));
        p.doTick();h.assertTrue(p.getArmorValue()>0&&p.getAbsorptionAmount()==8,"real armor and eight absorption active before cost");
        var c=new ItemStack(UnityFeastMod.CIGARETTE.get(),2);c.finishUsingItem(l,p);
        h.assertTrue(c.getCount()==1&&p.getHealth()==16&&p.getAbsorptionAmount()==8,"cigarette costs4red armor resistance absorption cannot substitute");
        effect(h,p,MobEffects.SPEED,600,1);effect(h,p,MobEffects.JUMP_BOOST,600,1);effect(h,p,MobEffects.NAUSEA,200,0);effect(h,p,MobEffects.BLINDNESS,200,0);
        h.assertTrue(!p.hasEffect(MobEffects.POISON)&&p.getFoodData().getFoodLevel()==20&&p.getFoodData().getSaturationLevel()==7,"no inherited poison/no nutrition");
        p.setGameMode(GameType.CREATIVE);p.setHealth(20);int count=c.getCount();c.finishUsingItem(l,p);
        h.assertTrue(c.getCount()==count&&p.getHealth()==20,"creative standard nonconsumption/invulnerability");p.setGameMode(GameType.SURVIVAL);
        // Actual continuous offhand use: effect appears on the32nd tick only.
        p.removeAllEffects();p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(UnityFeastMod.TOBACCO.get(),2));
        p.startUsingItem(InteractionHand.OFF_HAND);for(int tick=0;tick<31;tick++)p.doTick();
        h.assertTrue(p.getOffhandItem().getCount()==2&&!p.hasEffect(MobEffects.POISON),"31 actual use ticks no effects");p.doTick();
        h.assertTrue(p.getOffhandItem().getCount()==1&&p.hasEffect(MobEffects.POISON),"32nd actual use tick consumes once offhand");
        System.out.println("V130 PASS smoke nutrition, cancellation, full hunger, effects, armor/resistance/absorption red cost and actual32tick offhand use");h.succeed();
    }
    public static void deaths(GameTestHelper h){
        var l=h.getLevel();var pos=h.absolutePos(BlockPos.ZERO);
        var p=KitchenTests.player(h,pos);UnityHeartService.consume(p);UnityHeartService.consumeSoup(p);
        p.setHealth(3);p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.TOTEM_OF_UNDYING));p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(UnityFeastMod.CIGARETTE.get(),2));
        p.getMainHandItem().finishUsingItem(l,p);
        h.assertTrue(p.isAlive()&&p.getOffhandItem().isEmpty()&&p.getMainHandItem().getCount()==1&&p.getMaxHealth()==42,"cigarette totem consumed, remaining stack1 and old22health bonus preserved");
        for(boolean keep:List.of(false,true)){
            l.getGameRules().set(GameRules.KEEP_INVENTORY,keep,l.getServer());var dying=KitchenTests.player(h,pos.east(4));UnityHeartService.consume(dying);UnityHeartService.consumeSoup(dying);dying.setHealth(3);
            dying.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(UnityFeastMod.CIGARETTE.get(),2));dying.getMainHandItem().finishUsingItem(l,dying);
            h.assertTrue(dying.isDeadOrDying(),"cigarette true death below4");
            h.assertTrue(dying.getData(UnityFeastMod.HEART_DATA).effectiveBonus()==(keep?22:0),"death policy applies to both old rewards keep="+keep);
        }
        l.getGameRules().set(GameRules.KEEP_INVENTORY,false,l.getServer());
        System.out.println("V130 PASS cigarette totem and actual death keepInventory false/true retain old reward rules");h.succeed();
    }
}
