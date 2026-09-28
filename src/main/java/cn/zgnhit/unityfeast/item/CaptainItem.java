package cn.zgnhit.unityfeast.item;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
public final class CaptainItem extends Item {
    public CaptainItem(Properties properties){super(properties);}
    @Override public boolean canBeHurtBy(ItemStack stack,DamageSource source){return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> lines,TooltipFlag flag){
        lines.accept(Component.translatable("tooltip.unity_feast.captain").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("tooltip.unity_feast.captain_flight").withStyle(ChatFormatting.GRAY));
    }
}
