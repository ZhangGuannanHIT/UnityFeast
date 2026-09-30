package cn.zgnhit.unityfeast.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The standard consumable completes and consumes before the potentially lethal cost. */
public final class CigaretteItem extends Item {
    public CigaretteItem(Properties properties) { super(properties); }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        ItemStack remaining = super.finishUsingItem(stack, level, user);
        if (level instanceof ServerLevel server) {
            user.hurtServer(server, new DamageSource(server.registryAccess()
                    .lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(FoodDamage.CIGARETTE)), 4);
        }
        return remaining;
    }
}
