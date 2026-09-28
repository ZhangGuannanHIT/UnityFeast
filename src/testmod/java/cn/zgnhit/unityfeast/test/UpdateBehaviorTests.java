package cn.zgnhit.unityfeast.test;

import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.entity.Rat;
import cn.zgnhit.unityfeast.player.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForgeMod;
import java.util.*;

public final class UpdateBehaviorTests {
    public static void foodsAndRecipes(GameTestHelper h) {
        var l=h.getLevel();var p=UpdateGameTests.player(h);
        var old=new CompoundTag();old.putLong("hearts",3);
        p.setData(UnityFeastMod.HEART_DATA,UnityHeartData.SERIALIZER.read(p,TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),old)));
        h.assertValueEqual(p.getMaxHealth(),26F,"old hearts stay counts, absent soup defaults zero");
        p.addEffect(new MobEffectInstance(MobEffects.POISON,600));p.addEffect(new MobEffectInstance(MobEffects.SPEED,600,2));
        p.setHealth(3);p.getFoodData().setFoodLevel(20);p.causeFoodExhaustion(3);
        var soup=new ItemStack(UnityFeastMod.WEIJIXIAN.get(),3);soup.finishUsingItem(l,p);
        h.assertValueEqual(p.getMaxHealth(),46F,"three old hearts plus soup = 46");
        h.assertValueEqual(p.getHealth(),46F,"heal after attribute increase");h.assertValueEqual(p.getFoodData().getFoodLevel(),20,"full hunger");
        h.assertValueEqual(p.getFoodData().getSaturationLevel(),20F,"full saturation");
        var food=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());p.getFoodData().addAdditionalSaveData(food);
        h.assertValueEqual(food.buildResult().getFloatOr("foodExhaustionLevel",-1),0F,"exhaustion cleared");
        for(var e:List.of(MobEffects.RESISTANCE,MobEffects.FIRE_RESISTANCE,MobEffects.REGENERATION,MobEffects.ABSORPTION)) {
            h.assertValueEqual(p.getEffect(e).getAmplifier(),1,"effect II");h.assertValueEqual(p.getEffect(e).getDuration(),2400,"effect 120 seconds");
        }
        h.assertTrue(!p.hasEffect(MobEffects.POISON)&&p.hasEffect(MobEffects.SPEED),"only harmful effects removed");
        soup.finishUsingItem(l,p);h.assertValueEqual(p.getMaxHealth(),66F,"second soup adds another 20");h.assertValueEqual(soup.getCount(),1,"one unit per completed use");
        p.setHealth(31);for(int i=0;i<3;i++) UnityHeartService.reconcile(p);h.assertValueEqual(p.getHealth(),31F,"reconcile never heals");h.assertValueEqual(p.getMaxHealth(),66F,"reconcile never stacks twice");
        var data=p.getData(UnityFeastMod.HEART_DATA);
        h.assertValueEqual(new UnityHeartData(data.hearts(),data.soups(),1).afterClone(true).effectiveBonus(),0.0,"death clear both sources");
        h.assertValueEqual(new UnityHeartData(data.hearts(),data.soups(),2).afterClone(true).effectiveBonus(),46.0,"keep inventory keeps both sources");
        p.removeAllEffects();p.getFoodData().setFoodLevel(0);var san=new ItemStack(UnityFeastMod.SAN_ZHI.get(),2);san.finishUsingItem(l,p);
        h.assertValueEqual(p.getFoodData().getFoodLevel(),2,"san zhi nutrition");h.assertValueEqual(p.getEffect(MobEffects.POISON).getDuration(),60,"san zhi poison");h.assertValueEqual(p.getEffect(MobEffects.NAUSEA).getDuration(),60,"san zhi nausea");h.assertValueEqual(p.getHealth(),31F,"san zhi no instant damage");
        List<ItemStack> grid=new ArrayList<>(List.of(new ItemStack(UnityFeastMod.HOTPOT.get()),new ItemStack(UnityFeastMod.STINKY_FISH.get()),new ItemStack(UnityFeastMod.SAN_ZHI.get()),new ItemStack(UnityFeastMod.TABLE_DUMPLING.get()),new ItemStack(UnityFeastMod.TABLE_DUMPLING.get()),ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY));
        var rng=new Random(110);for(int i=0;i<60;i++) {Collections.shuffle(grid,rng);var recipe=l.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(3,3,grid),l);h.assertTrue(recipe.isPresent()&&recipe.get().value().assemble(CraftingInput.of(3,3,grid)).is(UnityFeastMod.WEIJIXIAN.get()),"shapeless layout "+i);}
        grid=new ArrayList<>(List.of(new ItemStack(UnityFeastMod.HOTPOT.get()),new ItemStack(UnityFeastMod.STINKY_FISH.get()),new ItemStack(UnityFeastMod.SAN_ZHI.get()),new ItemStack(UnityFeastMod.TABLE_DUMPLING.get(),2),ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY));
        h.assertTrue(l.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(3,3,grid),l).isEmpty(),"two dumplings in one slot not two ingredients");
        var menu=new CraftingMenu(12,p.getInventory(),ContainerLevelAccess.create(l,p.blockPosition()));p.containerMenu=menu;
        for(int i=0;i<9;i++) menu.getSlot(i+1).set(new ItemStack(i==4?UnityFeastMod.WEIJIXIAN.get():UnityFeastMod.UNITY_HEART.get(),2));
        menu.slotsChanged(menu.getSlot(1).container);
        h.assertTrue(menu.getSlot(0).getItem().is(UnityFeastMod.CAPTAIN.get()),"captain shaped result");menu.quickMoveStack(p,0);
        for(int i=1;i<=9;i++)h.assertValueEqual(menu.getSlot(i).getItem().getCount(),1,"shift craft exact ingredient count");
        // The real recipe-book placement path must split the two identical ingredients into two slots.
        var menu2=new CraftingMenu(13,p.getInventory(),ContainerLevelAccess.create(l,p.blockPosition()));p.containerMenu=menu2;p.getInventory().clearContent();
        p.getInventory().setItem(0,new ItemStack(UnityFeastMod.HOTPOT.get(),3));p.getInventory().setItem(1,new ItemStack(UnityFeastMod.STINKY_FISH.get(),3));p.getInventory().setItem(2,new ItemStack(UnityFeastMod.SAN_ZHI.get(),3));p.getInventory().setItem(3,new ItemStack(UnityFeastMod.TABLE_DUMPLING.get(),6));
        var recipe=l.getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,UnityFeastMod.id("weijixian"))).orElseThrow();
        menu2.handlePlacement(true,false,recipe,l,p.getInventory());
        h.assertValueEqual(menu2.getInputGridSlots().stream().filter(s->s.getItem().is(UnityFeastMod.TABLE_DUMPLING.get())).count(),2L,"recipe book splits two table dumplings");
        for(int n=0;n<3;n++){h.assertTrue(menu2.getSlot(0).getItem().is(UnityFeastMod.WEIJIXIAN.get()),"batch result present");menu2.quickMoveStack(p,0);}
        h.assertTrue(menu2.getInputGridSlots().stream().allMatch(s->s.getItem().isEmpty()),"three batch crafts consume exactly 3+3+3+6 ingredients");
        System.out.println("V110 PASS recipes: 60 permutations, duplicate-slot rejection, captain shift consumption; food values and mixed reward 26/46/66");h.succeed();
    }
    public static void mixedDeaths(GameTestHelper h) {
        for(boolean keep:List.of(false,true)) {
            var p=UpdateGameTests.player(h);UnityHeartService.consume(p);UnityHeartService.consumeSoup(p);
            h.getLevel().getGameRules().set(GameRules.KEEP_INVENTORY,keep,h.getLevel().getServer());
            p.setHealth(4);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(UnityFeastMod.STINKY_FISH.get(),2));p.getMainHandItem().finishUsingItem(h.getLevel(),p);
            h.assertTrue(p.isDeadOrDying(),"stinky fish lethal follows normal death");
            h.getLevel().getGameRules().set(GameRules.KEEP_INVENTORY,!keep,h.getLevel().getServer());
            var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());p.saveWithoutId(saved);
            var loaded=UpdateGameTests.player(h);loaded.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),saved.buildResult()));
            var respawn=UpdateGameTests.player(h);respawn.restoreFrom(loaded,false);
            h.assertValueEqual(respawn.getData(UnityFeastMod.HEART_DATA).effectiveBonus(),keep?22.0:0.0,"mixed death snapshot survives quitting before respawn");
            h.assertValueEqual(respawn.getMaxHealth(),keep?42F:20F,"mixed death maximum");
        }
        System.out.println("V110 PASS lethal food, mixed reward keep/clear and logout-before-respawn");h.succeed();
    }
    public static void flightAndProtection(GameTestHelper h) {
        var l=h.getLevel();var p=UpdateGameTests.player(h);var c=new ItemStack(UnityFeastMod.CAPTAIN.get());
        p.setItemSlot(EquipmentSlot.MAINHAND,c);p.doTick();h.assertTrue(!p.mayFly(),"main hand no flight");
        p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,c);p.doTick();h.assertTrue(p.mayFly(),"offhand enables standard flight");
        p.getAbilities().flying=true;p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.doTick();p.doTick();h.assertTrue(!p.mayFly()&&!p.getAbilities().flying,"removing only source revokes flying");
        var other=new AttributeModifier(UnityFeastMod.id("test_other_flight"),1,AttributeModifier.Operation.ADD_VALUE);p.getAttribute(NeoForgeMod.CREATIVE_FLIGHT).addTransientModifier(other);
        p.setItemSlot(EquipmentSlot.OFFHAND,c);p.doTick();p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.doTick();h.assertTrue(p.mayFly(),"other flight attribute survives");p.getAttribute(NeoForgeMod.CREATIVE_FLIGHT).removeModifier(other.id());
        p.setGameMode(GameType.CREATIVE);p.setItemSlot(EquipmentSlot.OFFHAND,c);p.doTick();p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.doTick();h.assertTrue(p.mayFly(),"creative survives removal");
        for(var damage:List.of(l.damageSources().inFire(),l.damageSources().lava(),l.damageSources().cactus(),l.damageSources().explosion(null,null))) {
            var item=new ItemEntity(l,p.getX(),p.getY(),p.getZ(),c.copy());l.addFreshEntity(item);h.assertTrue(!item.hurtServer(l,damage,100)&&item.isAlive(),"captain resists dropped item environmental damage");item.discard();
        }
        h.assertTrue(c.canBeHurtBy(l.damageSources().fellOutOfWorld()),"void remains normal");h.assertTrue(!c.isDamageableItem()&&c.getMaxStackSize()==1,"captain no durability stack one");
        System.out.println("V110 PASS offhand flight, removal, other source, creative and item environment immunity");h.succeed();
    }
    public static void ratCombat(GameTestHelper h) {
        var l=h.getLevel();l.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
        // Other tests use terrain beyond the tiny empty structure. Give combat a clear arena.
        var arena=h.absolutePos(BlockPos.ZERO);
        for(int x=-1;x<=5;x++)for(int z=-1;z<=5;z++) {
            l.setBlock(arena.offset(x,1,z),Blocks.STONE.defaultBlockState(),3);
            for(int y=2;y<=5;y++)l.setBlock(arena.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);
        }
        var rat=h.spawn(UnityFeastMod.RAT.get(),new Vec3(1,2,1));rat.setNoAi(true);var other=h.spawn(UnityFeastMod.RAT.get(),new Vec3(2,2,1));other.setNoAi(true);
        h.assertValueEqual(rat.getMaxHealth(),10F,"rat ten health");
        h.assertTrue(rat.getTarget()==null,"neutral before attacked");rat.hurtServer(l,l.damageSources().cactus(),1);h.assertTrue(rat.getTarget()==null,"environment no retaliation");
        rat.invulnerableTime=0;rat.hurtServer(l,l.damageSources().mobAttack(other),1);h.assertTrue(rat.getTarget()==other&&rat.redEyes(),"same species individual retaliation and red eyes");h.assertTrue(other.getTarget()==null,"no group alert");
        var arrow=new Arrow(EntityType.ARROW,l);arrow.setOwner(other);rat.setTarget(null);rat.invulnerableTime=0;rat.hurtServer(l,l.damageSources().arrow(arrow,other),1);h.assertTrue(rat.getTarget()==other,"projectile owner retaliation");
        other.setPos(rat.getX()+1,rat.getY(),rat.getZ());
        h.assertTrue(!rat.isWithinMeleeAttackRange(other)&&!rat.doHurtTarget(l,other),"separated collision boxes cannot attack even within old reach");
        h.assertValueEqual(other.getHealth(),10F,"out of contact causes no damage");
        h.assertValueEqual(rat.lastAttackTick(),Long.MIN_VALUE,"out of contact does not consume attack cooldown");
        AABB box=rat.getBoundingBox();
        for(AABB touching:List.of(
                new AABB(box.maxX,box.minY,box.minZ,box.maxX+.4,box.maxY,box.maxZ),
                new AABB(box.minX-.4,box.minY,box.minZ,box.minX,box.maxY,box.maxZ),
                new AABB(box.minX,box.maxY,box.minZ,box.maxX,box.maxY+.3,box.maxZ),
                new AABB(box.minX,box.minY-.3,box.minZ,box.maxX,box.minY,box.maxZ),
                new AABB(box.minX,box.minY,box.maxZ,box.maxX,box.maxY,box.maxZ+.4),
                new AABB(box.minX,box.minY,box.minZ-.4,box.maxX,box.maxY,box.minZ))) {
            other.setBoundingBox(touching);h.assertTrue(rat.isWithinMeleeAttackRange(other),"touching collision box faces are in range");
            other.setBoundingBox(touching.deflate(.0001));h.assertTrue(!rat.isWithinMeleeAttackRange(other),"any gap between collision boxes is out of range");
        }
        other.setPos(rat.getX()+.3,rat.getY(),rat.getZ());
        h.assertTrue(rat.hasLineOfSight(other),"combat fixture has an unobstructed line of sight");
        h.assertTrue(rat.isWithinMeleeAttackRange(other)&&rat.doHurtTarget(l,other),"overlapping collision boxes can attack");
        h.assertValueEqual(other.getHealth(),9F,"contact attack retains one base damage");
        long first=rat.lastAttackTick();float health=other.getHealth();
        rat.setTarget(null);rat.setTarget(other);h.assertTrue(!rat.doHurtTarget(l,other)&&other.getHealth()==health,"switching target does not reset cooldown");
        h.runAfterDelay(19,()->{
            other.setPos(rat.getX()+.3,rat.getY(),rat.getZ());
            h.assertTrue(rat.isWithinMeleeAttackRange(other)&&!rat.doHurtTarget(l,other),"contact cannot bypass twenty tick cooldown");
        });
        h.runAfterDelay(20,()->{
            other.setPos(rat.getX()+.3,rat.getY(),rat.getZ());other.invulnerableTime=0;
            h.assertTrue(rat.doHurtTarget(l,other),"contact attack resumes after cooldown");
            h.assertTrue(rat.lastAttackTick()-first>=20,"attack timestamps at least twenty ticks");
            other.setPos(rat.getX()+1,rat.getY(),rat.getZ());
            var wall=BlockPos.containing(rat.getX()+.5,rat.getY(),rat.getZ());l.setBlock(wall,Blocks.STONE.defaultBlockState(),3);
            h.assertTrue(!rat.hasLineOfSight(other)&&!rat.doHurtTarget(l,other),"solid wall blocks attack");
            l.setBlock(wall,Blocks.AIR.defaultBlockState(),3);other.discard();rat.tick();h.assertTrue(rat.getTarget()==null,"removed target cleared");
            for(int light:new int[]{6,7,9,10}) h.assertValueEqual(Rat.redAt(light,false),light<7,"red threshold");
            var dead=h.spawn(UnityFeastMod.RAT.get(),new Vec3(1,2,3));dead.setNoAi(true);dead.hurtServer(l,l.damageSources().generic(),100);
            var drops=l.getEntitiesOfClass(ItemEntity.class,dead.getBoundingBox().inflate(1));h.assertTrue(drops.stream().filter(i->i.getItem().is(UnityFeastMod.SAN_ZHI.get())).mapToInt(i->i.getItem().getCount()).sum()==1,"normal environmental death drops exactly one san zhi");
            System.out.println("V110 PASS rat neutrality, environment/melee/projectile attribution, collision contact, wall and cooldown timestamps "+first+"/"+rat.lastAttackTick());h.succeed();
        });
    }
    public static void flightLifecycle(GameTestHelper h) {
        for(boolean keep:List.of(false,true)) {
            var p=UpdateGameTests.player(h);p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(UnityFeastMod.CAPTAIN.get()));p.doTick();
            h.getLevel().getGameRules().set(GameRules.KEEP_INVENTORY,keep,h.getLevel().getServer());
            p.hurtServer(p.level(),p.damageSources().genericKill(),Float.MAX_VALUE);
            var respawn=UpdateGameTests.player(h);respawn.restoreFrom(p,false);respawn.doTick();
            h.assertValueEqual(respawn.mayFly(),keep,"captain death inventory controls restored flight");
            h.assertTrue(!respawn.getAbilities().flying,"respawn never forces flight");
        }
        var p=UpdateGameTests.player(h);p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(UnityFeastMod.CAPTAIN.get()));p.doTick();
        var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());p.saveWithoutId(saved);
        var copy=UpdateGameTests.player(h);copy.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),saved.buildResult()));copy.doTick();
        h.assertTrue(copy.mayFly(),"saved offhand restores flight");
        var origin=copy.position();for(var dim:List.of(net.minecraft.world.level.Level.NETHER,net.minecraft.world.level.Level.END,net.minecraft.world.level.Level.OVERWORLD)) {
            copy.teleportTo(h.getLevel().getServer().getLevel(dim),origin.x,100,origin.z,Set.of(),0,0,true);copy.doTick();h.assertTrue(copy.mayFly(),"dimension retains offhand eligibility");
        }
        var hand=copy.getOffhandItem();copy.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);copy.setItemSlot(EquipmentSlot.MAINHAND,hand);copy.doTick();h.assertTrue(!copy.mayFly(),"hand swap revokes flight");
        for(var mode:List.of(GameType.CREATIVE,GameType.SPECTATOR,GameType.SURVIVAL,GameType.ADVENTURE)){copy.setGameMode(mode);copy.doTick();h.assertValueEqual(copy.mayFly(),mode==GameType.CREATIVE||mode==GameType.SPECTATOR,"game mode eligibility after removal");}
        System.out.println("V110 PASS captain death both rules, respawn, disk roundtrip, dimensions, swap and modes");h.succeed();
    }
    public static void ratJump(GameTestHelper h) {
        var l=h.getLevel();var base=h.absolutePos(new BlockPos(0,2,0));
        for(int x=-1;x<=12;x++)for(int z=0;z<=4;z++) {l.setBlock(base.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);for(int y=0;y<=3;y++)l.setBlock(base.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);}
        for(int z=0;z<=4;z++)l.setBlock(base.offset(4,0,z),Blocks.STONE.defaultBlockState(),3);
        var rat=h.spawn(UnityFeastMod.RAT.get(),new Vec3(.5,2,1.5));var rabbit=h.spawn(EntityType.RABBIT,new Vec3(.5,2,3.5));
        rat.removeFreeWill();rabbit.removeFreeWill();h.runAfterDelay(5,()->{rat.getNavigation().moveTo(base.getX()+10.5,base.getY(),base.getZ()+1.5,.6);rabbit.getNavigation().moveTo(base.getX()+10.5,base.getY(),base.getZ()+3.5,.6);});
        final double[] maxY={rat.getY()};final boolean[] airborne={false};
        for(int sample:new int[]{40,80,120})h.runAtTickTime(sample,()->System.out.println("V110 COURSE rat="+(rat.getX()-base.getX())+" rabbit="+(rabbit.getX()-base.getX())));
        for(int t=1;t<170;t++)h.runAtTickTime(t,()->{rat.tick();rabbit.tick();maxY[0]=Math.max(maxY[0],rat.getY());if(!rat.onGround()&&rat.getDeltaMovement().y>0)airborne[0]=true;});
        h.runAtTickTime(175,()->{
            System.out.println("V110 movement rat dx="+(rat.getX()-base.getX()-.5)+" rabbit dx="+(rabbit.getX()-base.getX()-.5)+" peak jump="+(maxY[0]-base.getY())+" airborne="+airborne[0]);
            h.assertTrue(rat.getX()>base.getX()+4.7&&maxY[0]>base.getY()+1&&airborne[0],"real jump crosses full block");h.succeed();
        });
    }
}
