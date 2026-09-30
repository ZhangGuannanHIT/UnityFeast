package cn.zgnhit.unityfeast.combat;

import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.entity.BottleCapProjectile;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Only bottle-cap damage and the source created by a Xingqing melee attack are handled here. */
@EventBusSubscriber(modid = UnityFeastMod.ID)
public final class BottleCombat {
    public static final ResourceKey<DamageType> BOTTLE_CAP = ResourceKey.create(Registries.DAMAGE_TYPE, UnityFeastMod.id("bottle_cap"));
    private BottleCombat() {}

    @SubscribeEvent public static void fixedDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(BOTTLE_CAP)) {
            for (var reduction : new DamageContainer.Reduction[]{DamageContainer.Reduction.ARMOR,
                    DamageContainer.Reduction.MOB_EFFECTS, DamageContainer.Reduction.ENCHANTMENTS,
                    DamageContainer.Reduction.INVULNERABILITY})
                event.addReductionModifier(reduction, (container, amount) -> 0F);
            // Vanilla still rejects damage <= lastHurt during hurt immunity before reductions run.
            // If it accepts a stronger cap, do not turn its fixed four into only the difference.
            // Absorption, shields, immunity rejection and cancellation remain normal.
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void impact(ProjectileImpactEvent event) {
        if (event.getProjectile() instanceof BottleCapProjectile cap) cap.observeImpact(event);
    }

    @SubscribeEvent public static void effects(LivingDamageEvent.Post event) {
        var source = event.getSource();
        if (event.getEntity().level().isClientSide()
                || (!source.is(BOTTLE_CAP) && !(source instanceof XingqingMeleeSource))) return;
        if (DamagePostAccess.effectiveDamage(event) <= 0) return;
        var target = event.getEntity();
        if (source.is(BOTTLE_CAP) && source.getDirectEntity() instanceof BottleCapProjectile cap
                && cap.claimEffectHit(target)) {
            target.addEffect(new MobEffectInstance(MobEffects.SPEED, 100), source.getEntity());
            target.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100), source.getEntity());
            target.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 100), source.getEntity());
        } else if (source instanceof XingqingMeleeSource melee && melee.claim(target)) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60), source.getEntity());
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60), source.getEntity());
        }
    }

}
