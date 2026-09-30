package cn.zgnhit.unityfeast.item;

import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.combat.BottleCapLaunch;
import cn.zgnhit.unityfeast.combat.XingqingMeleeSource;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public final class XingqingItem extends Item {
    public XingqingItem(Properties properties) {
        // Iron sword behavior, enchantability and repair; 1 player + 4 baseline + 2 material = 7.
        super(properties.sword(ToolMaterial.IRON, 4F, -3F).durability(1062));
    }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!stack.is(UnityFeastMod.XINGQING) || player.isSpectator() || player.getCooldowns().isOnCooldown(stack))
            return InteractionResult.FAIL;
        if (level instanceof ServerLevel server) {
            if (!BottleCapLaunch.launch(server, player)) return InteractionResult.FAIL;
            // Vanilla cooldown groups are per player and shared by every stack and both hands.
            player.getCooldowns().addCooldown(stack, 20);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResult.SUCCESS;
    }

    @SuppressWarnings("deprecation")
    @Override public DamageSource getItemDamageSource(LivingEntity attacker) {
        var source = attacker instanceof Player p ? attacker.damageSources().playerAttack(p) : attacker.damageSources().mobAttack(attacker);
        return new XingqingMeleeSource(source, attacker.getWeaponItem());
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                          Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("tooltip.unity_feast.xingqing").withStyle(ChatFormatting.GRAY));
    }
}
