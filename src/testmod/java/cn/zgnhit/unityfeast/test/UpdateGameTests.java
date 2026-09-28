package cn.zgnhit.unityfeast.test;

import cn.zgnhit.unityfeast.*;
import cn.zgnhit.unityfeast.block.*;
import cn.zgnhit.unityfeast.player.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.*;
import java.util.*;

public final class UpdateGameTests {
    static ServerPlayer player(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);p.connection.markClientLoaded();
        h.getLevel().getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION,false,h.getLevel().getServer()); return p;
    }
    public static void tableUpgrade(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(1,2,1)); var level=h.getLevel();
        for(int mask=0;mask<16;mask++) {
            var old=UnityFeastMod.TABLE.get().defaultBlockState();
            for(int i=0;i<4;i++) old=old.setValue(TableBlock.SLOTS[i],(mask&(1<<i))!=0);
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),3); level.setBlock(pos,old,3);
            var be=(TableBlockEntity)level.getBlockEntity(pos);
            // First operation is the native break loot lookup, before any inventory access.
            var loot=Block.getDrops(old,level,pos,be);
            h.assertValueEqual(loot.stream().filter(s->s.is(UnityFeastMod.DUMPLING.get())).mapToInt(ItemStack::getCount).sum(),Integer.bitCount(mask),"first-break legacy migration");
            h.assertValueEqual(be.occupiedMask(),mask,"all 16 old corner combinations preserved");
            for(int i=0;i<4;i++) be.put(i,ItemStack.EMPTY);
            var saved=be.saveCustomOnly(level.registryAccess());
            for(int repeat=0;repeat<3;repeat++) {
                var loaded=new TableBlockEntity(pos,old); loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),saved));
                h.assertValueEqual(loaded.occupiedMask(),0,"completed migration cannot refill empty slots even with stale legacy properties");
                saved=loaded.saveCustomOnly(level.registryAccess());
            }
        }
        var p=player(h); var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        for(var fish:List.of(Items.COD,Items.SALMON,Items.TROPICAL_FISH,Items.PUFFERFISH)) {
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),3); level.setBlock(pos,UnityFeastMod.TABLE.get().defaultBlockState(),3);
            var stack=new ItemStack(fish,2);stack.set(DataComponents.CUSTOM_NAME,Component.literal("original fish data"));p.setItemInHand(InteractionHand.MAIN_HAND,stack);
            level.getBlockState(pos).useItemOn(stack,level,p,InteractionHand.MAIN_HAND,hit);
            h.assertValueEqual(stack.getCount(),1,"fish placement deducts exactly one");
            var be=(TableBlockEntity)level.getBlockEntity(pos);
            var saved=be.saveCustomOnly(level.registryAccess());var loaded=new TableBlockEntity(pos,be.getBlockState());loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),saved));
            var drops=Block.getDrops(be.getBlockState(),level,pos,loaded);
            h.assertTrue(drops.stream().anyMatch(s->s.is(fish)&&s.getCount()==1&&s.has(DataComponents.CUSTOM_NAME)),"breaking preserves original fish identity and data");
            level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3)).forEach(ItemEntity::discard);
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);level.getBlockState(pos).useWithoutItem(level,p,hit);
            var result=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3));
            h.assertTrue(result.size()==1&&result.getFirst().getItem().is(UnityFeastMod.STINKY_FISH.get())&&!result.getFirst().getItem().has(DataComponents.CUSTOM_NAME),"all raw fish convert to standard stinky fish");
        }
        for(var item:List.of(Items.COOKED_COD,Items.COOKED_SALMON,Items.COD_BUCKET,Items.PUFFERFISH_BUCKET,UnityFeastMod.TABLE_DUMPLING.get(),UnityFeastMod.STINKY_FISH.get()))
            h.assertTrue(!new ItemStack(item).is(UnityFeastMod.RAW_FISH),"invalid inputs excluded from raw-fish tag");
        p.setHealth(20);p.getFoodData().setFoodLevel(0);p.getAttribute(Attributes.ARMOR).setBaseValue(30);
        p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,1000,4));p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,1000,3));
        float absorption=p.getAbsorptionAmount();var food=new ItemStack(UnityFeastMod.STINKY_FISH.get(),2);food.finishUsingItem(level,p);
        h.assertValueEqual(p.getHealth(),16F,"stinky fish removes four red health through armor and resistance");
        h.assertValueEqual(p.getAbsorptionAmount(),absorption,"yellow hearts do not pay for stinky fish damage");
        h.assertValueEqual(food.getCount(),1,"food consumed once");h.assertValueEqual(p.getFoodData().getFoodLevel(),2,"stinky fish nutrition");
        h.assertValueEqual(p.getEffect(MobEffects.BLINDNESS).getDuration(),200,"blindness duration");
        h.assertValueEqual(p.getEffect(MobEffects.NAUSEA).getDuration(),200,"nausea duration");
        UnityHeartService.consume(p);p.setHealth(4);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.TOTEM_OF_UNDYING));
        new ItemStack(UnityFeastMod.STINKY_FISH.get()).finishUsingItem(level,p);
        h.assertTrue(p.isAlive()&&p.getData(UnityFeastMod.HEART_DATA).effectiveHearts()==1,"stinky fish uses vanilla totem and does not erase heart reward");
        System.out.println("V110 PASS 16 legacy masks, immediate break, repeated empty reload, original fish data, fixed red-health damage and totem");
        h.succeed();
    }
}
