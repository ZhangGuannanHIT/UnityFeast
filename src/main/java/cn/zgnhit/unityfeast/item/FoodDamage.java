package cn.zgnhit.unityfeast.item;

import cn.zgnhit.unityfeast.UnityFeastMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class FoodDamage {
    public static final ResourceKey<DamageType> STINKY_FISH=ResourceKey.create(Registries.DAMAGE_TYPE,UnityFeastMod.id("stinky_fish"));
    public static final ResourceKey<DamageType> CIGARETTE=ResourceKey.create(Registries.DAMAGE_TYPE,UnityFeastMod.id("cigarette"));
    private FoodDamage() {}
    @SubscribeEvent public static void incoming(LivingIncomingDamageEvent event) {
        if(event.getSource().is(STINKY_FISH) || event.getSource().is(CIGARETTE))
            for(var reduction:DamageContainer.Reduction.values()) event.addReductionModifier(reduction,(container,amount)->0F);
    }
}
