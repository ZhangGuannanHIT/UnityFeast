package cn.zgnhit.unityfeast.combat;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** A per-attack source: vanilla passes this same object to its primary and sweeping hits. */
public final class XingqingMeleeSource extends DamageSource {
    private final ItemStack weapon;
    private final Set<Integer> affected = new HashSet<>();

    public XingqingMeleeSource(DamageSource vanilla, ItemStack weapon) {
        super(vanilla.typeHolder(), vanilla.getDirectEntity(), vanilla.getEntity(), vanilla.sourcePositionRaw());
        this.weapon = weapon.copy();
    }

    @Override public ItemStack getWeaponItem() { return weapon; }
    public boolean claim(LivingEntity target) { return affected.add(target.getId()); }
}
