package cn.zgnhit.unityfeast.item;

import cn.zgnhit.unityfeast.player.UnityHeartService;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public final class FeastFoodItem extends Item {
    public enum Kind { TABLE_DUMPLING, UNITY_HEART, WEIJIXIAN }
    private final Kind kind;
    public FeastFoodItem(Properties p, Kind kind) { super(p); this.kind = kind; }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide()) {
            if (kind == Kind.TABLE_DUMPLING || kind == Kind.WEIJIXIAN) {
                var harmful = user.getActiveEffects().stream().filter(e -> e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                        .map(MobEffectInstance::getEffect).toList();
                harmful.forEach(user::removeEffect);
                if(kind==Kind.TABLE_DUMPLING) {
                    user.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300));
                    user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300));
                } else {
                    if(user instanceof ServerPlayer player) UnityHeartService.consumeSoup(player);
                    for(var effect:java.util.List.of(MobEffects.RESISTANCE,MobEffects.FIRE_RESISTANCE,MobEffects.REGENERATION,MobEffects.ABSORPTION))
                        user.addEffect(new MobEffectInstance(effect,2400,1));
                }
            } else if (user instanceof ServerPlayer player) {
                UnityHeartService.consume(player);
            }
        }
        // Food/Consumable alone handle nutrition, statistics and one-item consumption.
        return super.finishUsingItem(stack, level, user);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("tooltip.unity_feast." + kind.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY));
    }
}
