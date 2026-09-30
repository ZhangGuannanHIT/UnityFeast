package cn.zgnhit.unityfeast.test;

import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.combat.BottleCombat;
import cn.zgnhit.unityfeast.entity.BottleCapProjectile;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Headless server object/physics tests. These do not claim two graphical clients were connected. */
public final class BottleCombatTests {
    private static final class Cap extends BottleCapProjectile {
        Cap(GameTestHelper h, LivingEntity owner) { super(h.getLevel(), owner); }
        void hit(LivingEntity target) { onHit(new EntityHitResult(target)); }
        void wall(BlockPos pos) { onHit(new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)); }
    }
    private static ServerPlayer player(GameTestHelper h, int offset) {
        return KitchenTests.player(h, h.absolutePos(new BlockPos(offset, 3, 1)));
    }
    private static LivingEntity cow(GameTestHelper h, int x) {
        var cow = h.spawn(EntityType.COW, new BlockPos(x, 4, 3));
        cow.setNoAi(true);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        cow.setHealth(100);
        return cow;
    }
    private static int caps(GameTestHelper h, Player p) {
        return h.getLevel().getEntitiesOfClass(BottleCapProjectile.class, p.getBoundingBox().inflate(20)).size();
    }
    private static void clearCaps(GameTestHelper h, Player p) {
        h.getLevel().getEntitiesOfClass(BottleCapProjectile.class, p.getBoundingBox().inflate(50)).forEach(BottleCapProjectile::discard);
    }
    private static void effects(GameTestHelper h, LivingEntity target, boolean expected) {
        for (var effect : List.of(MobEffects.SPEED, MobEffects.STRENGTH, MobEffects.JUMP_BOOST)) {
            var instance = target.getEffect(effect);
            h.assertTrue(expected ? instance != null && instance.getAmplifier() == 0 && instance.getDuration() == 100 : instance == null,
                    "cap effect " + effect + " expected=" + expected);
        }
    }

    public static void launches(GameTestHelper h) {
        var l = h.getLevel(); var p = player(h, 1); var q = player(h, 4);
        try {
            clearCaps(h, p);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(UnityFeastMod.BOTTLE_CAP.get(), 2));
            UnityFeastMod.BOTTLE_CAP.get().use(l, p, InteractionHand.MAIN_HAND);
            h.assertValueEqual(p.getMainHandItem().getCount(), 1, "one cap consumed");
            h.assertValueEqual(caps(h, p), 1, "one projectile per invocation");
            UnityFeastMod.BOTTLE_CAP.get().use(l, p, InteractionHand.MAIN_HAND);
            UnityFeastMod.BOTTLE_CAP.get().use(l, p, InteractionHand.MAIN_HAND);
            h.assertTrue(p.getMainHandItem().isEmpty(), "last cap consumed");
            h.assertValueEqual(caps(h, p), 2, "empty hand cannot throw again; no one-second cap cooldown");
            p.setGameMode(GameType.CREATIVE);
            p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(UnityFeastMod.BOTTLE_CAP.get()));
            UnityFeastMod.BOTTLE_CAP.get().use(l, p, InteractionHand.OFF_HAND);
            h.assertValueEqual(p.getOffhandItem().getCount(), 1, "creative cap retained");
            p.setGameMode(GameType.SURVIVAL); clearCaps(h, p);

            var first = new ItemStack(UnityFeastMod.XINGQING.get());
            first.enchant(l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 5);
            first.enchant(l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FIRE_ASPECT), 2);
            first.enchant(l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING), 3);
            p.setItemInHand(InteractionHand.MAIN_HAND, first);
            p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(UnityFeastMod.XINGQING.get()));
            UnityFeastMod.XINGQING.get().use(l, p, InteractionHand.MAIN_HAND);
            h.assertValueEqual(caps(h, p), 1, "no ammunition required");
            for (int tick = 1; tick <= 19; tick++) {
                p.getCooldowns().tick();
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(UnityFeastMod.XINGQING.get()));
                UnityFeastMod.XINGQING.get().use(l, p, InteractionHand.MAIN_HAND);
                UnityFeastMod.XINGQING.get().use(l, p, InteractionHand.OFF_HAND);
            }
            h.assertValueEqual(caps(h, p), 1, "19 ticks, changed stacks and hands cannot bypass cooldown");
            q.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(UnityFeastMod.XINGQING.get()));
            UnityFeastMod.XINGQING.get().use(l, q, InteractionHand.MAIN_HAND);
            h.assertValueEqual(caps(h, p), 2, "another player's cooldown independent");
            p.getCooldowns().tick();
            UnityFeastMod.XINGQING.get().use(l, p, InteractionHand.OFF_HAND);
            h.assertValueEqual(caps(h, p), 3, "20th tick permits exactly next shot");
            h.assertValueEqual(first.getDamageValue(), 0, "launch never consumes durability");
            for (var cap : l.getEntitiesOfClass(BottleCapProjectile.class, p.getBoundingBox().inflate(20)))
                h.assertTrue(cap.getWeaponItem().isEmpty() && !cap.isOnFire(), "launcher enchantments not copied to cap");

            p.getCooldowns().removeCooldown(UnityFeastMod.id("xingqing"));
            Consumer<EntityJoinLevelEvent> cancel = e -> { if (e.getEntity() instanceof BottleCapProjectile) e.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(cancel);
            try {
                UnityFeastMod.XINGQING.get().use(l, p, InteractionHand.MAIN_HAND);
                h.assertTrue(!p.getCooldowns().isOnCooldown(p.getMainHandItem()), "canceled spawn does not start cooldown");
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(UnityFeastMod.BOTTLE_CAP.get()));
                UnityFeastMod.BOTTLE_CAP.get().use(l, p, InteractionHand.MAIN_HAND);
                h.assertValueEqual(p.getMainHandItem().getCount(), 1, "canceled spawn does not consume material");
            } finally { NeoForge.EVENT_BUS.unregister(cancel); }
            System.out.println("V130 PASS cap launches: consumption/creative/shared 20-tick cooldown/two server players/canceled spawn");
        } finally { clearCaps(h, p); l.getServer().getPlayerList().remove(p); l.getServer().getPlayerList().remove(q); }
        h.succeed();
    }

    public static void damage(GameTestHelper h) {
        var l = h.getLevel(); var p = player(h, 1); var target = cow(h, 3);
        try {
            var cap = new Cap(h, p); cap.hit(target); cap.hit(target);
            h.assertTrue(target.getHealth() == 96 && cap.isRemoved(), "one hit is exactly four; repeated collision ignored"); effects(h, target, true);
            h.assertTrue(target.getLastDamageSource().getEntity() == p && target.getLastDamageSource().getDirectEntity() == cap, "real owner/direct attribution");
            target.removeAllEffects(); new Cap(h, p).hit(target);
            h.assertTrue(target.getHealth() == 96, "second cap respects hurt invulnerability"); effects(h, target, false);
            target.invulnerableTime = 0;
            target.hurtServer(l, l.damageSources().generic(), 1);
            float afterSmallHit = target.getHealth();
            var afterSmallCap = new Cap(h, p); afterSmallCap.hit(target);
            h.assertTrue(target.getHealth() == afterSmallHit - 4, "accepted cap after one damage still deals fixed four, not delta three");
            target.removeAllEffects();
            var repeatedCap = new Cap(h, p); repeatedCap.hit(target);
            h.assertTrue(target.getHealth() == afterSmallHit - 4 && repeatedCap.isRemoved(), "another four-damage cap is still rejected by normal hurt immunity");
            effects(h, target, false);
            target.setHealth(96);
            target.invulnerableTime = 0;
            target.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
            target.getItemBySlot(EquipmentSlot.CHEST).enchant(l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION), 4);
            target.tick();
            h.assertTrue(target.getArmorValue() > 0, "target is actually wearing active armor");
            target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 4));
            p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 4));
            new Cap(h, p).hit(target);
            h.assertTrue(target.getHealth() == 92, "armor/protection/resistance and thrower strength do not alter four damage");
            target.invulnerableTime = 0; target.removeAllEffects();
            target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
            float oldAbsorb = target.getAbsorptionAmount();
            new Cap(h, p).hit(target);
            h.assertTrue(target.getHealth() == 92 && target.getAbsorptionAmount() == oldAbsorb - 4, "absorption pays cap damage"); effects(h, target, true);

            target.invulnerableTime = 0; target.removeAllEffects();
            Consumer<LivingIncomingDamageEvent> cancel = e -> { if (e.getEntity() == target && e.getSource().is(BottleCombat.BOTTLE_CAP)) e.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(cancel);
            try { var canceled = new Cap(h, p); canceled.hit(target); h.assertTrue(canceled.isRemoved() && target.getHealth() == 92, "cancel cleans cap without damage"); effects(h, target, false); }
            finally { NeoForge.EVENT_BUS.unregister(cancel); }

            target.removeAllEffects(); target.invulnerableTime = 0;
            for (var effect : List.of(MobEffects.SPEED, MobEffects.STRENGTH, MobEffects.JUMP_BOOST))
                target.addEffect(new MobEffectInstance(effect, 300, 2));
            new Cap(h, p).hit(target);
            for (var effect : List.of(MobEffects.SPEED, MobEffects.STRENGTH, MobEffects.JUMP_BOOST))
                h.assertTrue(target.getEffect(effect).getAmplifier() == 2 && target.getEffect(effect).getDuration() == 300, "cap does not downgrade stronger effect");
            target.removeAllEffects();
            target.setHealth(2); target.invulnerableTime = 0;
            target.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            new Cap(h, p).hit(target);
            h.assertTrue(target.isAlive() && target.getOffhandItem().isEmpty(), "normal totem protection");
            System.out.println("V130 PASS cap damage: exact four, armor/protection/resistance, absorption, cancellation, owner, invulnerability, totem");
        } finally { target.discard(); l.getServer().getPlayerList().remove(p); }
        h.succeed();
    }

    public static void permissions(GameTestHelper h) {
        var l = h.getLevel(); var p = player(h, 1); var q = player(h, 4);
        boolean oldPvp = l.getGameRules().get(GameRules.PVP);
        var oldDifficulty = l.getDifficulty();
        try {
            l.getGameRules().set(GameRules.PVP, true, l.getServer());
            for (var difficulty : Difficulty.values()) {
                l.getServer().setDifficulty(difficulty, true);
                q.setHealth(20); q.invulnerableTime = 0; q.removeAllEffects();
                new Cap(h, p).hit(q);
                h.assertTrue(q.getHealth() == 16, "fixed four against player in " + difficulty); effects(h, q, true);
            }
            q.setHealth(20); q.invulnerableTime = 0; q.removeAllEffects();
            l.getGameRules().set(GameRules.PVP, false, l.getServer());
            new Cap(h, p).hit(q); h.assertTrue(q.getHealth() == 20, "PVP false respected"); effects(h, q, false);
            l.getGameRules().set(GameRules.PVP, true, l.getServer());
            q.setGameMode(GameType.CREATIVE); new Cap(h, p).hit(q);
            h.assertTrue(q.getHealth() == 20, "creative immunity respected"); effects(h, q, false);
            q.setGameMode(GameType.SPECTATOR); new Cap(h, p).hit(q);
            h.assertTrue(q.getHealth() == 20, "spectator immunity respected"); effects(h, q, false);
            q.setGameMode(GameType.SURVIVAL);
            q.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
            q.startUsingItem(InteractionHand.OFF_HAND);
            for (int i = 0; i < 8; i++) q.doTick();
            q.setYRot(0); q.setYHeadRot(0);
            var shielded = new Cap(h, p); shielded.setPos(q.getX(), q.getY() + .5, q.getZ() + 2); shielded.hit(q);
            h.assertTrue(q.getHealth() == 20 && shielded.isRemoved(), "front shield blocks and cap disappears"); effects(h, q, false);
            q.stopUsingItem();
            var team = l.getScoreboard().addPlayerTeam("cap_test_team"); team.setAllowFriendlyFire(false);
            l.getScoreboard().addPlayerToTeam(p.getScoreboardName(), team); l.getScoreboard().addPlayerToTeam(q.getScoreboardName(), team);
            try { new Cap(h, p).hit(q); h.assertTrue(q.getHealth() == 20, "team friendly-fire false respected"); effects(h, q, false); }
            finally { l.getScoreboard().removePlayerTeam(team); }
            System.out.println("V130 PASS cap permissions: PVP/creative/spectator/shield/team friendly-fire");
        } finally { l.getServer().setDifficulty(oldDifficulty, true); l.getGameRules().set(GameRules.PVP, oldPvp, l.getServer()); l.getServer().getPlayerList().remove(p); l.getServer().getPlayerList().remove(q); }
        h.succeed();
    }

    public static void persistence(GameTestHelper h) {
        var l = h.getLevel(); var p = player(h, 1);
        try {
            var material = new ItemEntity(l, p.getX(), p.getY(), p.getZ(), new ItemStack(UnityFeastMod.BOTTLE_CAP.get()));
            l.addFreshEntity(material);
            var thrown = new Cap(h, p); thrown.setPos(p.getX(), p.getY() + 20, p.getZ()); thrown.setNoGravity(true); l.addFreshEntity(thrown);
            for (int i = 0; i < 120; i++) thrown.tick();
            var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, l.registryAccess()); thrown.saveWithoutId(saved); thrown.discard();
            var restored = new Cap(h, p); restored.load(TagValueInput.create(ProblemReporter.DISCARDING, l.registryAccess(), saved.buildResult()));
            for (int i = 0; i < 79; i++) restored.tick();
            h.assertTrue(!restored.isRemoved(), "age 199 survives saved/reloaded"); restored.tick(); h.assertTrue(restored.isRemoved(), "saved lifetime ends at 200");
            var tag = saved.buildResult(); tag.putLong("CapExpiresAt", l.getGameTime() - 1);
            var expired = new Cap(h, p); expired.load(TagValueInput.create(ProblemReporter.DISCARDING, l.registryAccess(), tag)); expired.tick();
            h.assertTrue(expired.isRemoved(), "expired while unloaded is cleared at reload");
            var falling = new Cap(h, p); falling.setPos(p.getX(), l.getMinY() - 80, p.getZ()); falling.tick();
            h.assertTrue(falling.isRemoved(), "normal void removal");
            var movingTarget = cow(h, 6); movingTarget.setPos(p.getX() + 8, p.getY() + 20, p.getZ());
            var moving = new Cap(h, p); moving.setPos(movingTarget.getX() - 3, movingTarget.getY() + .6, movingTarget.getZ());
            moving.setDeltaMovement(1, 0, 0); l.addFreshEntity(moving);
            for (int i = 0; i < 10 && !moving.isRemoved(); i++) moving.tick();
            h.assertTrue(moving.isRemoved() && movingTarget.getHealth() == 96, "real movement ray-collides once with living entity"); movingTarget.discard();
            var canceledTarget = cow(h, 6); canceledTarget.setPos(p.getX() + 8, p.getY() + 20, p.getZ());
            var canceledShot = new Cap(h, p); canceledShot.setPos(canceledTarget.getX() - 3, canceledTarget.getY() + .6, canceledTarget.getZ());
            canceledShot.setDeltaMovement(1, 0, 0); l.addFreshEntity(canceledShot);
            int[] canceledImpacts = {0};
            Consumer<ProjectileImpactEvent> cancelImpact = event -> {
                if (event.getProjectile() == canceledShot) { canceledImpacts[0]++; event.setCanceled(true); }
            };
            // Registered after the production listener at the same LOWEST priority: verify final event state.
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ProjectileImpactEvent.class, cancelImpact);
            try {
                for (int i = 0; i < 10 && !canceledShot.isRemoved(); i++) canceledShot.tick();
                h.assertTrue(canceledImpacts[0] == 1 && canceledShot.isRemoved() && canceledTarget.getHealth() == 100,
                        "canceled physical impact cleans cap without damage even when a later listener cancels");
                effects(h, canceledTarget, false);
            } finally { NeoForge.EVENT_BUS.unregister(cancelImpact); canceledTarget.discard(); canceledShot.discard(); }
            var wall = p.blockPosition().offset(12, 20, 0); l.setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
            var wallShot = new Cap(h, p); wallShot.setPos(Vec3.atCenterOf(wall).add(-3, 0, 0)); wallShot.setDeltaMovement(1, 0, 0); l.addFreshEntity(wallShot);
            for (int i = 0; i < 10 && !wallShot.isRemoved(); i++) wallShot.tick();
            h.assertTrue(wallShot.isRemoved(), "real movement ray-collides with wall and disappears"); l.removeBlock(wall, false);
            for (int i = 0; i < 100; i++) { var impact = new Cap(h, p); impact.wall(p.blockPosition().below()); h.assertTrue(impact.isRemoved(), "block impact cleanup"); }
            var drops = l.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(30)).stream().filter(e -> e.getItem().is(UnityFeastMod.BOTTLE_CAP)).toList();
            h.assertTrue(drops.size() == 1 && drops.getFirst() == material && material.getItem().getCount() == 1, "100 impacts/lifetime never return cap material or remove ordinary drop");
            material.discard();
            System.out.println("V130 PASS cap save/reload age 120→199→200, elapsed expiry, 100 wall impacts, zero recoverable cap output");
        } finally { clearCaps(h, p); l.getServer().getPlayerList().remove(p); }
        h.succeed();
    }

    public static void melee(GameTestHelper h) {
        var l = h.getLevel(); var p = player(h, 1); var target = cow(h, 3); var nearby = cow(h, 4);
        try {
            var weapon = new ItemStack(UnityFeastMod.XINGQING.get()); p.setItemInHand(InteractionHand.MAIN_HAND, weapon);
            for (int i = 0; i < 25; i++) p.doTick();
            h.assertTrue(p.getAttributeValue(Attributes.ATTACK_DAMAGE) == 7 && p.getAttributeValue(Attributes.ATTACK_SPEED) == 1, "final attributes 7 damage / 1 speed");
            h.assertValueEqual(weapon.getMaxDamage(), 1062, "exact durability");
            h.assertTrue(weapon.get(DataComponents.ENCHANTABLE).equals(new ItemStack(Items.IRON_SWORD).get(DataComponents.ENCHANTABLE)), "iron sword enchantability");
            var enchantments = l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            for (var key : List.of(Enchantments.SHARPNESS, Enchantments.SMITE, Enchantments.BANE_OF_ARTHROPODS,
                    Enchantments.KNOCKBACK, Enchantments.FIRE_ASPECT, Enchantments.LOOTING, Enchantments.SWEEPING_EDGE,
                    Enchantments.UNBREAKING, Enchantments.MENDING, Enchantments.VANISHING_CURSE))
                h.assertTrue(weapon.supportsEnchantment(enchantments.getOrThrow(key)), "sword enchantment " + key);
            for (var key : List.of(Enchantments.POWER, Enchantments.INFINITY, Enchantments.MULTISHOT, Enchantments.LOYALTY))
                h.assertTrue(!weapon.supportsEnchantment(enchantments.getOrThrow(key)), "unrelated enchantment rejected " + key);
            h.assertTrue(weapon.get(DataComponents.REPAIRABLE).isValidRepairItem(new ItemStack(Items.IRON_INGOT)), "iron repair baseline");
            p.snapTo(target.getX(), target.getY(), target.getZ() - 1.5, 0, 0); p.setOnGround(true); p.fallDistance = 0;
            nearby.setPos(target.getX() + .8, target.getY(), target.getZ());
            p.attack(target);
            h.assertTrue(target.getHealth() == 93, "full cooldown noncritical main hit seven");
            h.assertTrue(target.hasEffect(MobEffects.WITHER) && target.getEffect(MobEffects.WITHER).getDuration() == 60 && target.hasEffect(MobEffects.SLOWNESS), "main hit debuffs");
            h.assertTrue(nearby.getHealth() < 100 && nearby.hasEffect(MobEffects.WITHER), "actual vanilla sweep receives debuffs");
            h.assertValueEqual(weapon.getDamageValue(), 1, "one melee durability");
            target.removeAllEffects(); target.invulnerableTime = 0;
            var arrow = new net.minecraft.world.entity.projectile.arrow.Arrow(EntityType.ARROW, l); arrow.setOwner(p);
            target.hurtServer(l, l.damageSources().arrow(arrow, p), 1);
            h.assertTrue(!target.hasEffect(MobEffects.WITHER), "holding weapon does not mark earlier projectile damage");
            target.invulnerableTime = 0;
            new Cap(h, p).hit(target);
            h.assertTrue(!target.hasEffect(MobEffects.WITHER) && !target.hasEffect(MobEffects.SLOWNESS), "remote cap has no melee debuffs");
            target.removeAllEffects(); target.invulnerableTime = 0;
            target.hurtServer(l, l.damageSources().wither(), 1);
            h.assertTrue(!target.hasEffect(MobEffects.WITHER), "wither DOT does not refresh melee effect");
            for (int i = 0; i < 25; i++) p.doTick(); target.invulnerableTime = 0; p.setOnGround(true); p.fallDistance = 0;
            weapon.setDamageValue(1061); p.attack(target);
            h.assertTrue(p.getMainHandItem().isEmpty() && target.hasEffect(MobEffects.WITHER), "last durability hit completes then breaks");
            lootIsolation(h, p, target);
            System.out.println("V130 PASS Xingqing: actual seven damage/one speed, iron repair, sword-only enchants, sweep, one wear, last-use break, DOT/remote isolation");
        } finally { target.discard(); nearby.discard(); l.getServer().getPlayerList().remove(p); }
        h.succeed();
    }

    private static LootContext lootContext(GameTestHelper h, LivingEntity victim, DamageSource source) {
        var params = new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.THIS_ENTITY, victim)
                .withParameter(LootContextParams.ORIGIN, victim.position()).withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity())
                .create(LootContextParamSets.ENTITY);
        return new LootContext.Builder(params).create(Optional.empty());
    }

    private static void lootIsolation(GameTestHelper h, ServerPlayer player, LivingEntity victim) {
        var l = h.getLevel();
        var looting = l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING);
        var enchanted = new ItemStack(UnityFeastMod.XINGQING.get()); enchanted.enchant(looting, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, enchanted);
        var capSource = new DamageSource(l.registryAccess().getOrThrow(BottleCombat.BOTTLE_CAP), new Cap(h, player), player);
        var meleeSource = l.damageSources().playerAttack(player);
        var count = EnchantedCountIncreaseFunction.lootingMultiplier(l.registryAccess(), ConstantValue.exactly(2)).build();
        h.assertValueEqual(count.apply(new ItemStack(Items.WHEAT), lootContext(h, victim, capSource)).getCount(), 1,
                "actual cap loot count function has no held-weapon Looting III bonus");
        h.assertValueEqual(count.apply(new ItemStack(Items.WHEAT), lootContext(h, victim, meleeSource)).getCount(), 7,
                "ordinary melee loot still receives Looting III");
        var chance = new LootItemRandomChanceWithEnchantedBonusCondition(0, LevelBasedValue.constant(1), looting);
        h.assertTrue(!chance.test(lootContext(h, victim, capSource)) && chance.test(lootContext(h, victim, meleeSource)),
                "actual loot chance condition uses cap zero-looting but normal melee enchanted chance");
        float base = .085F;
        float capChance = EnchantmentHelper.processEquipmentDropChance(l, victim, capSource, base);
        float meleeChance = EnchantmentHelper.processEquipmentDropChance(l, victim, meleeSource, base);
        h.assertTrue(Math.abs(capChance - base) < .00001F && meleeChance > base,
                "actual equipment-drop calculation excludes only cap attacker Looting");
        System.out.println("V130 PASS cap Looting isolation: real count, probability and equipment-drop functions; ordinary melee bonuses preserved");
    }
}
