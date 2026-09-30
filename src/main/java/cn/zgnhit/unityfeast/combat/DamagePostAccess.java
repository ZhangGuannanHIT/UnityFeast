package cn.zgnhit.unityfeast.combat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** The supported 26.1 runtimes renamed only this post-event health-damage accessor. */
final class DamagePostAccess {
    private static final Method HEALTH_DAMAGE = findHealthDamage();
    private DamagePostAccess() {}

    private static Method findHealthDamage() {
        for (String name : new String[]{"getHealthDamage", "getNewDamage"}) {
            try { return LivingDamageEvent.Post.class.getMethod(name); }
            catch (NoSuchMethodException ignored) { /* Try the earlier 26.1 accessor. */ }
        }
        throw new ExceptionInInitializerError("Unsupported LivingDamageEvent.Post: no health-damage accessor");
    }

    static float effectiveDamage(LivingDamageEvent.Post event) {
        try {
            return ((Number) HEALTH_DAMAGE.invoke(event)).floatValue()
                    + event.getReduction(DamageContainer.Reduction.ABSORPTION);
        } catch (IllegalAccessException | InvocationTargetException error) {
            throw new IllegalStateException("Cannot read completed damage", error);
        }
    }
}
