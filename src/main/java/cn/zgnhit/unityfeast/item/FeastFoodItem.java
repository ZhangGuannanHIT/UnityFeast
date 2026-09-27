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
    public enum Kind { TABLE_DUMPLING, UNITY_HEART }
    private final Kind kind;
    public FeastFoodItem(Properties p, Kind kind) { super(p); this.kind = kind; }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide()) {
            if (kind == Kind.TABLE_DUMPLING) {
                var harmful = user.getActiveEffects().stream().filter(e -> e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                        .map(MobEffectInstance::getEffect).toList();
                harmful.forEach(user::removeEffect);
                user.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300));
                user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300));
            } else if (user instanceof ServerPlayer player) {
                UnityHeartService.consume(player);
            }
        }
        // Food/Consumable alone handle nutrition, statistics and one-item consumption.
        return super.finishUsingItem(stack, level, user);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("tooltip.unity_feast." + (kind == Kind.TABLE_DUMPLING ? "table_dumpling" : "unity_heart")).withStyle(ChatFormatting.GRAY));
    }
}
