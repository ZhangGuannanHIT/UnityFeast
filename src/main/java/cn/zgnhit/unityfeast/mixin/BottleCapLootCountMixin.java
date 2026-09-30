package cn.zgnhit.unityfeast.mixin;

import cn.zgnhit.unityfeast.combat.BottleCombat;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The same vanilla entry point exists before and after NeoForge added its entity-loot event. */
@Mixin(EnchantedCountIncreaseFunction.class)
public abstract class BottleCapLootCountMixin {
    @Shadow @Final private Holder<Enchantment> enchantment;

    @Inject(method = "run", at = @At("HEAD"), cancellable = true)
    private void unityFeast$noCapLooting(ItemStack stack, LootContext context, CallbackInfoReturnable<ItemStack> callback) {
        var source = context.getOptionalParameter(LootContextParams.DAMAGE_SOURCE);
        if (source != null && source.is(BottleCombat.BOTTLE_CAP) && enchantment.is(Enchantments.LOOTING))
            callback.setReturnValue(stack);
    }
}
