package cn.zgnhit.unityfeast.test;

import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.block.TableBlock;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;

/** Development fixture only: two real network clients exercise the shipped JAR. */
public final class AcceptanceServer {
    static final Path CONTROL = Path.of(System.getProperty("unity_feast.project", "."), ".tools", "acceptance");
    private static String phase="WAIT";
    private static int age;
    private static final BlockPos TABLE=new BlockPos(0,64,0);
    private static boolean initialized;
    private static int caught, beetles;
    private static boolean coveredPool;
    private static final java.lang.reflect.Field NIBBLE;
    static {
        try { NIBBLE=FishingHook.class.getDeclaredField("nibble"); NIBBLE.setAccessible(true); }
        catch(ReflectiveOperationException e){throw new ExceptionInInitializerError(e);}
    }
    @SubscribeEvent public static void fished(ItemFishedEvent event) {
        if(Boolean.getBoolean("unity_feast.acceptance")&&phase.equals("FISH")&&event.getEntity().getName().getString().equals("FeastOne")) {
            caught++;
            if(event.getDrops().stream().anyMatch(s->s.is(UnityFeastMod.WATER_BEETLE.get()))) beetles++;
            System.out.println("ACCEPTANCE natural fishing catch="+caught+" covered="+coveredPool+" loot="+event.getDrops());
        }
    }
    private static void pool(ServerPlayer p, boolean covered) {
        var level=p.level(); int cx=covered?24:0;
        for(int x=cx-6;x<=cx+6;x++) for(int z=-21;z<=-6;z++) {
            level.setBlock(new BlockPos(x,59,z),Blocks.STONE.defaultBlockState(),3);
            boolean water=Math.abs(x-cx)<(covered?5:3)&&z>=-19&&z<=-10;
            for(int y=60;y<=62;y++) level.setBlock(new BlockPos(x,y,z),(water?Blocks.WATER:Blocks.STONE).defaultBlockState(),3);
            for(int y=63;y<=68;y++) level.setBlock(new BlockPos(x,y,z),(covered&&y==68?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
        }
        var rod=new ItemStack(Items.FISHING_ROD);
        rod.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LURE),3);
        p.getInventory().setItem(0,rod); inventory(p);
        p.teleportTo(level,cx+.5,63,-8,Set.of(),180F,10F,true);
    }
    private static void phase(String next) throws Exception {
        phase=next; age=0;
        Files.createDirectories(CONTROL); Files.writeString(CONTROL.resolve("phase.txt"),next);
        System.out.println("ACCEPTANCE phase="+next);
    }
    private static void check(boolean ok,String description) {
        if(!ok) throw new AssertionError(description);
        System.out.println("ACCEPTANCE PASS "+description);
    }
    private static void inventory(ServerPlayer p) { p.inventoryMenu.broadcastChanges(); p.containerMenu.broadcastChanges(); }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) throws Exception {
        if(!Boolean.getBoolean("unity_feast.acceptance")) return;
        var server=event.getServer();
        if(!initialized) { initialized=true; phase(Files.exists(CONTROL.resolve("saved.txt"))?"REJOIN":"WAIT"); }
        if(Files.exists(CONTROL.resolve("stop.txt"))) { server.halt(false); return; }
        var one=server.getPlayerList().getPlayerByName("FeastOne");
        var two=server.getPlayerList().getPlayerByName("FeastTwo");
        if(phase.equals("LEAVE") && one==null && two==null) {
            Files.writeString(CONTROL.resolve("saved.txt"),"26.1.2.78 two-player persistence checkpoint");
            phase("SAVED"); server.halt(false); return;
        }
        if(one==null||two==null||!one.connection.hasClientLoaded()||!two.connection.hasClientLoaded()) return;
        age++;
        try {
            var level=server.overworld();
            switch(phase) {
                case "WAIT" -> {
                    level.getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION,false,server);
                    level.getGameRules().set(GameRules.SPAWN_MOBS,false,server);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set day");
                    for(int x=-9;x<=9;x++) for(int z=-9;z<=9;z++) level.setBlock(new BlockPos(x,63,z),Blocks.SMOOTH_STONE.defaultBlockState(),3);
                    level.setBlock(TABLE,UnityFeastMod.TABLE.get().defaultBlockState(),3);
                    for(var p:List.of(one,two)) {
                        p.setGameMode(GameType.SURVIVAL); p.getInventory().clearContent(); p.setHealth(20);
                        p.getFoodData().setFoodLevel(20); p.getFoodData().setSaturation(0);
                        p.getInventory().setItem(0,new ItemStack(UnityFeastMod.DUMPLING.get(),4)); inventory(p);
                    }
                    one.teleportTo(level,2.8,64,3.5,Set.of(),140F,26F,true);
                    two.teleportTo(level,-2.0,64,2.8,Set.of(),220F,26F,true);
                    phase("EMPTY");
                }
                case "EMPTY" -> { if(age>100) phase("PUT"); }
                case "PUT" -> {
                    if(age>120) {
                        check(TableBlock.mask(level.getBlockState(TABLE))==15,"two network clients fill exactly four slots");
                        check(one.getMainHandItem().getCount()+two.getMainHandItem().getCount()==4,"eight initial dumplings minus four placements equals four; rejected clicks do not consume");
                        check(!one.isUsingItem()&&!two.isUsingItem(),"full table never starts eating"); phase("FULL");
                    }
                }
                case "FULL" -> { if(age>100) { for(var p:List.of(one,two)){p.getInventory().setItem(0,ItemStack.EMPTY); inventory(p);} phase("TAKE"); } }
                case "TAKE" -> {
                    if(age>120) {
                        check(TableBlock.mask(level.getBlockState(TABLE))==0,"two clients empty table without stale-state duplication");
                        int total=level.getEntitiesOfClass(ItemEntity.class,new AABB(TABLE).inflate(10)).stream().filter(e->e.getItem().is(UnityFeastMod.TABLE_DUMPLING.get())).mapToInt(e->e.getItem().getCount()).sum();
                        for(var p:List.of(one,two)) total+=p.getInventory().countItem(UnityFeastMod.TABLE_DUMPLING.get());
                        check(total==4,"exactly four extracted table dumplings across inventories and world entities");
                        one.getInventory().clearContent(); two.getInventory().clearContent();
                        Item[] items={UnityFeastMod.TABLE_ITEM.get(),UnityFeastMod.DUMPLING.get(),UnityFeastMod.TABLE_DUMPLING.get(),UnityFeastMod.WATER_BEETLE.get(),UnityFeastMod.HOTPOT.get(),UnityFeastMod.UNITY_HEART.get()};
                        for(int i=0;i<items.length;i++) one.getInventory().setItem(9+i,new ItemStack(items[i]));
                        inventory(one); inventory(two); phase("INVENTORY");
                    }
                }
                case "INVENTORY" -> {
                    if(age>120) {
                        for(var p:List.of(one,two)) {
                            p.getInventory().clearContent(); p.getInventory().setItem(0,new ItemStack(UnityFeastMod.UNITY_HEART.get(),3));
                            p.setHealth(2); p.getFoodData().setFoodLevel(1); p.getFoodData().setSaturation(0); inventory(p);
                        }
                        phase("HEARTS");
                    }
                }
                case "HEARTS" -> {
                    if(age>220) {
                        check(one.getMaxHealth()==26&&one.getHealth()==26,"real 32-tick food use: three hearts give 26/26 health");
                        check(two.getMaxHealth()==22&&two.getHealth()==22,"second network player has independent 22/22 health");
                        check(one.getFoodData().getFoodLevel()==20&&one.getFoodData().getSaturationLevel()==20,"heart restores food and saturation after full consumption");
                        phase("HEART_PHOTO");
                    }
                }
                case "HEART_PHOTO" -> {
                    if(age>100) {
                        one.setHealth(23); two.setHealth(17);
                        level.setBlock(TABLE,UnityFeastMod.TABLE.get().defaultBlockState().setValue(TableBlock.SLOTS[1],true).setValue(TableBlock.SLOTS[3],true),3);
                        server.getPlayerList().saveAll(); server.saveAllChunks(false,true,true);
                        phase("LEAVE");
                    }
                }
                case "REJOIN" -> {
                    if(age>80) {
                        check(one.getMaxHealth()==26&&one.getHealth()==23,"first player logout + dedicated server restart preserves bonus and partial health");
                        check(two.getMaxHealth()==22&&two.getHealth()==17,"second player's independent bonus survives restart without healing");
                        check(TableBlock.mask(level.getBlockState(TABLE))==10,"table corner bits survive server restart");
                        Files.writeString(CONTROL.resolve("passed.txt"),"two-client interactions, full food use, logout/reconnect and dedicated-server restart passed");
                        pool(one,false); phase("FISH");
                    }
                }
                case "FISH" -> {
                    if(caught>=4&&!coveredPool) {coveredPool=true; pool(one,true);}
                    if(caught>=8) {
                        check(beetles>0&&beetles<8,"natural bobber ticks and successful rod retrieval produce both beetles and original loot");
                        Files.writeString(CONTROL.resolve("fishing-passed.txt"),"8 natural successful catches; beetles="+beetles+"; first 4 open pool, last 4 covered pool");
                        phase("DONE");
                    } else if(one.fishing==null) {
                        if(age%30==0) one.getMainHandItem().use(level,one,InteractionHand.MAIN_HAND);
                    } else if(NIBBLE.getInt(one.fishing)>0) {
                        // Observe only; never alter nibble, timing, RNG or loot. Retrieve through the real rod.
                        one.getMainHandItem().use(level,one,InteractionHand.MAIN_HAND);
                    }
                    if(age>10000) throw new AssertionError("Natural fishing timed out");
                }
            }
        } catch(Throwable t) {
            System.err.println("ACCEPTANCE FAIL "+t); t.printStackTrace();
            Files.writeString(CONTROL.resolve("failed.txt"),t.toString()); phase("FAILED");
        }
    }
}
