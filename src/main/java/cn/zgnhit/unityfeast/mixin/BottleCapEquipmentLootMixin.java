package cn.zgnhit.unityfeast.mixin;

import cn.zgnhit.unityfeast.combat.BottleCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnchantmentHelper.class)
public abstract class BottleCapEquipmentLootMixin {
    // Only the second iteration (the attacker) is filtered; victim enchantments remain untouched.
    @Redirect(method = "processEquipmentDropChance", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;runIterationOnEquipment(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V"))
    private static void unityFeast$noCapEquipmentLooting(LivingEntity attacker, EnchantmentHelper.EnchantmentInSlotVisitor visitor,
            ServerLevel level, LivingEntity victim, DamageSource source, float chance) {
        if (!source.is(BottleCombat.BOTTLE_CAP)) {
            EnchantmentHelper.runIterationOnEquipment(attacker, visitor);
            return;
        }
        EnchantmentHelper.runIterationOnEquipment(attacker, (enchantment, strength, item) -> {
            if (!enchantment.is(Enchantments.LOOTING)) visitor.accept(enchantment, strength, item);
        });
    }
}
