package cn.zgnhit.unityfeast.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class StinkyFishItem extends Item {
    public StinkyFishItem(Properties properties) { super(properties); }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity user) {
        // Consume before potentially lethal damage: death inventory contains the remaining stack only.
        var remaining=super.finishUsingItem(stack,level,user);
        if(level instanceof ServerLevel server) {
            user.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,200));
            user.addEffect(new MobEffectInstance(MobEffects.NAUSEA,200));
            user.hurtServer(server,new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(FoodDamage.STINKY_FISH)),4);
        }
        return remaining;
    }
}
