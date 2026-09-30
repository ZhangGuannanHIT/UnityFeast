package cn.zgnhit.unityfeast.mixin;

import cn.zgnhit.unityfeast.combat.BottleCombat;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LootItemRandomChanceWithEnchantedBonusCondition.class)
public abstract class BottleCapLootChanceMixin {
    @Shadow public abstract Holder<Enchantment> enchantment();
    @Shadow public abstract float unenchantedChance();

    @Inject(method = "test(Lnet/minecraft/world/level/storage/loot/LootContext;)Z", at = @At("HEAD"), cancellable = true)
    private void unityFeast$noCapLooting(LootContext context, CallbackInfoReturnable<Boolean> callback) {
        var source = context.getOptionalParameter(LootContextParams.DAMAGE_SOURCE);
        if (source != null && source.is(BottleCombat.BOTTLE_CAP) && enchantment().is(Enchantments.LOOTING))
            callback.setReturnValue(context.getRandom().nextFloat() < unenchantedChance());
    }
}
