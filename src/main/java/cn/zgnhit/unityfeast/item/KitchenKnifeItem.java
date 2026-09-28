package cn.zgnhit.unityfeast.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import cn.zgnhit.unityfeast.UnityFeastMod;

/** Standard weapon component handles attack durability, exactly once per hit. */
@EventBusSubscriber(modid=UnityFeastMod.ID)
public final class KitchenKnifeItem extends Item {
    public KitchenKnifeItem(Properties p) { super(p); }
    @Override public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) { return false; }
    @Override public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) { return false; }
    @SubscribeEvent public static void anvil(AnvilUpdateEvent event) {
        // Vanilla explicitly overrides applicability for creative anvils. Close only that knife path.
        if(event.getLeft().is(UnityFeastMod.KITCHEN_KNIFE) && !event.getRight().isEmpty()
                && (event.getRight().is(Items.ENCHANTED_BOOK)||event.getOutput().isEnchanted())) event.setCanceled(true);
    }
}
