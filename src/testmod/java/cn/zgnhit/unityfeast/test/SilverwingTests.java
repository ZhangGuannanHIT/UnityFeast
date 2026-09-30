package cn.zgnhit.unityfeast.test;

import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.armor.SilverwingArmor;
import cn.zgnhit.unityfeast.player.SilverwingStrengthService;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Actual registered armor, vanilla effect events/ticks, and player persistence/clone paths. */
public final class SilverwingTests {
    private SilverwingTests() {}
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static Item[] armor() { return new Item[]{UnityFeastMod.SILVERWING_HELMET.get(), UnityFeastMod.SILVERWING_CHESTPLATE.get(),
            UnityFeastMod.SILVERWING_LEGGINGS.get(), UnityFeastMod.SILVERWING_BOOTS.get()}; }
    private static ServerPlayer player(GameTestHelper helper) {
        var player = KitchenTests.player(helper, helper.absolutePos(new BlockPos(2, 3, 2)));
        player.setNoGravity(true);
        return player;
    }
    private static void wear(ServerPlayer player) {
        var armor = armor();
        for (int index = 0; index < 4; index++) player.setItemSlot(SLOTS[index], new ItemStack(armor[index]));
    }
    private static void tick(ServerPlayer player, int count) { for (int tick = 0; tick < count; tick++) player.doTick(); }
    private static void effect(GameTestHelper helper, ServerPlayer player, int amplifier, int duration, String context) {
        var strength = player.getEffect(MobEffects.STRENGTH);
        helper.assertTrue(strength != null, context + " present");
        helper.assertValueEqual(strength.getAmplifier(), amplifier, context + " amplifier");
        helper.assertValueEqual(strength.getDuration(), duration, context + " duration");
    }
    public static void armor(GameTestHelper helper) {
        var ours = armor();
        var base = new Item[]{Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS};
        for (int index = 0; index < 4; index++) {
            var actual = new ItemStack(ours[index]); var expected = new ItemStack(base[index]);
            helper.assertValueEqual(actual.getMaxDamage(), expected.getMaxDamage(), "netherite durability " + SLOTS[index]);
            helper.assertValueEqual(actual.getMaxStackSize(), 1, "armor is unstackable");
            helper.assertTrue(actual.get(DataComponents.ATTRIBUTE_MODIFIERS).equals(expected.get(DataComponents.ATTRIBUTE_MODIFIERS)), "netherite defense toughness knockback modifiers");
            helper.assertTrue(actual.get(DataComponents.ENCHANTABLE).equals(expected.get(DataComponents.ENCHANTABLE)), "netherite enchantability");
            helper.assertTrue(actual.get(DataComponents.REPAIRABLE).equals(expected.get(DataComponents.REPAIRABLE)), "netherite repair ingredient");
            helper.assertTrue(actual.get(DataComponents.DAMAGE_RESISTANT).equals(expected.get(DataComponents.DAMAGE_RESISTANT)), "netherite fire immunity");
            helper.assertTrue(actual.isValidRepairItem(new ItemStack(Items.NETHERITE_INGOT)), "netherite repairs armor");
            helper.assertTrue(!actual.isValidRepairItem(new ItemStack(UnityFeastMod.BOTTLE_CAP.get())), "caps do not repair armor");
            helper.assertTrue(actual.is(ItemTags.TRIMMABLE_ARMOR), "armor supports normal smithing trim");
            var equipped = actual.get(DataComponents.EQUIPPABLE); var vanilla = expected.get(DataComponents.EQUIPPABLE);
            helper.assertTrue(equipped.slot() == vanilla.slot() && equipped.equipSound().equals(vanilla.equipSound())
                    && equipped.dispensable() == vanilla.dispensable() && equipped.swappable() == vanilla.swappable()
                    && equipped.damageOnHurt() == vanilla.damageOnHurt() && equipped.equipOnInteract() == vanilla.equipOnInteract(), "netherite wear behavior");
            helper.assertTrue(equipped.assetId().orElseThrow().equals(SilverwingArmor.MATERIAL.assetId())
                    && !equipped.assetId().equals(vanilla.assetId()), "dedicated green equipment asset");
            for (var enchantment : helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().toList())
                helper.assertTrue(actual.supportsEnchantment(enchantment) == expected.supportsEnchantment(enchantment), "enchantment support parity " + enchantment.getRegisteredName());
            System.out.println("V130 SILVERWING armor=" + ours[index] + " durability=" + actual.getMaxDamage() + " attributes=" + actual.get(DataComponents.ATTRIBUTE_MODIFIERS));
        }
        var player = player(helper); wear(player); tick(player, 1);
        helper.assertValueEqual(player.getArmorValue(), 20, "full netherite armor points");
        helper.assertValueEqual(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 12D, "full netherite toughness");
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) - .4) < 1e-6, "full netherite knockback resistance");
        var other = player(helper);
        for (int index = 0; index < 4; index++) other.setItemSlot(SLOTS[index], new ItemStack(base[index]));
        tick(other, 1);
        player.hurtServer(player.level(), player.damageSources().generic(), 8);
        other.hurtServer(other.level(), other.damageSources().generic(), 8);
        for (var slot : SLOTS) helper.assertValueEqual(player.getItemBySlot(slot).getDamageValue(), other.getItemBySlot(slot).getDamageValue(), "normal armor wear parity");
        helper.assertTrue(!player.getAbilities().mayfly, "silverwing never grants creative flight");
        helper.succeed();
    }

    public static void effects(GameTestHelper helper) {
        var player = player(helper);
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 150, 0)); tick(player, 5);
        effect(helper, player, 0, 145, "unsuited external remains vanilla");
        wear(player); tick(player, 1); effect(helper, player, 0, -1, "complete suit infinite strength I");
        helper.assertValueEqual(player.getAttributeValue(Attributes.ATTACK_DAMAGE), 4D, "only one strength modifier");
        tick(player, 9); player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); tick(player, 1);
        effect(helper, player, 0, 134, "external I continues ticking behind suit");
        player.removeAllEffects(); wear(player); tick(player, 1);
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100, 0)); tick(player, 12);
        player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY); tick(player, 1);
        effect(helper, player, 0, 87, "external I applied after equipping survives removal");

        player.removeAllEffects(); wear(player); tick(player, 1);
        UnityFeastMod.DIPPED_LETTUCE.get().getDefaultInstance().finishUsingItem(player.level(), player);
        effect(helper, player, 1, 200, "actual dipped lettuce grants II while suited");
        tick(player, 17); player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY); tick(player, 1);
        effect(helper, player, 1, 182, "removing suit preserves external II exact remainder");
        wear(player); tick(player, 182); effect(helper, player, 0, -1, "expired II returns to suit I");
        player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY); tick(player, 1);
        helper.assertTrue(!player.hasEffect(MobEffects.STRENGTH), "no external source means no residual strength");

        // The external chain has a long I underneath a short II before the armor is equipped.
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100, 0));
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20, 1));
        wear(player); tick(player, 10); player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); tick(player, 1);
        effect(helper, player, 1, 9, "external hidden chain survives suit transition");
        tick(player, 9); effect(helper, player, 0, 80, "hidden external I restores without freezing");

        player.removeAllEffects(); wear(player); tick(player, 1);
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100, 1)); tick(player, 5);
        Items.MILK_BUCKET.getDefaultInstance().finishUsingItem(player.level(), player);
        helper.assertTrue(!player.hasEffect(MobEffects.STRENGTH), "actual milk removes external and displayed strength");
        tick(player, 1); effect(helper, player, 0, -1, "complete equipment restores only suit I after milk");
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); tick(player, 1);
        helper.assertTrue(!player.hasEffect(MobEffects.STRENGTH), "milk-cleared external II never resurfaces");
        for (int cycle = 0; cycle < 16; cycle++) for (var slot : SLOTS) {
            wear(player); tick(player, 1); effect(helper, player, 0, -1, "rapid equip");
            helper.assertValueEqual(player.getAttributeValue(Attributes.ATTACK_DAMAGE), 4D, "rapid equip does not stack");
            player.setItemSlot(slot, ItemStack.EMPTY); tick(player, 1);
            helper.assertTrue(!player.hasEffect(MobEffects.STRENGTH), "every missing armor slot removes contribution");
        }
        wear(player); tick(player, 1);
        var boots = player.getItemBySlot(EquipmentSlot.FEET); boots.setDamageValue(boots.getMaxDamage() - 1);
        boots.hurtAndBreak(1, player, EquipmentSlot.FEET); tick(player, 1);
        helper.assertTrue(boots.isEmpty() && !player.hasEffect(MobEffects.STRENGTH), "broken armor immediately loses suit effect");
        System.out.println("V130 PASS silverwing effects: all slots, external I/II and hidden chain, actual lettuce/milk, exact ticking, 64 equip cycles, armor break");
        helper.succeed();
    }

    public static void lifecycle(GameTestHelper helper) {
        var original = player(helper); wear(original); tick(original, 1);
        original.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 150, 0));
        original.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 1)); tick(original, 7);
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, original.registryAccess()); original.saveWithoutId(output);
        var loaded = player(helper); loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, loaded.registryAccess(), output.buildResult()));
        SilverwingStrengthService.reconcile(loaded); effect(helper, loaded, 1, 33, "save reload preserves external II remainder");
        loaded.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); tick(loaded, 1); effect(helper, loaded, 1, 32, "reload then unequip no infinite residue");
        tick(loaded, 32); effect(helper, loaded, 0, 110, "saved hidden chain survives actual player serialization");

        // Save between removing equipment and the next tick: provenance must clean a persisted infinite suit effect.
        var stale = player(helper); wear(stale); tick(stale, 1); stale.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        var staleOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, stale.registryAccess()); stale.saveWithoutId(staleOutput);
        var staleLoad = player(helper); staleLoad.load(TagValueInput.create(ProblemReporter.DISCARDING, staleLoad.registryAccess(), staleOutput.buildResult()));
        SilverwingStrengthService.reconcile(staleLoad); helper.assertTrue(!staleLoad.hasEffect(MobEffects.STRENGTH), "unequipped save cannot leave infinite strength on reload");

        var endReturn = player(helper); endReturn.restoreFrom(original, true);
        effect(helper, endReturn, 1, 33, "non-death clone preserves independent source state");
        endReturn.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY); tick(endReturn, 1); effect(helper, endReturn, 1, 32, "End-return clone permits proper unequip");

        var rule = helper.getLevel().getGameRules().get(GameRules.KEEP_INVENTORY);
        for (boolean keep : List.of(false, true)) {
            var dying = player(helper); wear(dying); tick(dying, 1); dying.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100, 1));
            helper.getLevel().getGameRules().set(GameRules.KEEP_INVENTORY, keep, helper.getLevel().getServer());
            dying.hurtServer(dying.level(), dying.damageSources().genericKill(), Float.MAX_VALUE);
            var respawned = player(helper); respawned.restoreFrom(dying, false); tick(respawned, 1);
            helper.assertTrue(SilverwingStrengthService.isComplete(respawned) == keep, "death follows actual retained armor");
            if (keep) effect(helper, respawned, 0, -1, "retained gear grants only suit I, not dead player's II");
            else helper.assertTrue(!respawned.hasEffect(MobEffects.STRENGTH), "lost gear cannot preserve suit strength");
        }
        helper.getLevel().getGameRules().set(GameRules.KEEP_INVENTORY, rule, helper.getLevel().getServer());
        var traveler = player(helper); wear(traveler); tick(traveler, 1); traveler.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 90, 1));
        for (var dimension : List.of(Level.NETHER, Level.END, Level.OVERWORLD)) {
            var target = helper.getLevel().getServer().getLevel(dimension);
            helper.assertTrue(traveler.teleportTo(target, traveler.getX(), 100, traveler.getZ(), java.util.Set.of(), 0, 0, true), "actual dimension teleport");
            effect(helper, traveler, 1, 90, "dimension change neither refills nor loses external duration");
        }
        traveler.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); tick(traveler, 1); effect(helper, traveler, 1, 89, "dimension changes preserve external ownership");
        System.out.println("V130 PASS silverwing lifecycle: player save/reload, stale unequipped save, End return, death both keepInventory modes, actual dimension teleports");
        helper.succeed();
    }
}
