package cn.zgnhit.unityfeast.item;

import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.combat.BottleCapLaunch;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class BottleCapItem extends Item {
    public BottleCapItem(Properties properties) { super(properties); }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!stack.is(UnityFeastMod.BOTTLE_CAP) || player.isSpectator()) return InteractionResult.FAIL;
        if (level instanceof ServerLevel server) {
            if (!BottleCapLaunch.launch(server, player)) return InteractionResult.FAIL;
            stack.consume(1, player);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResult.SUCCESS;
    }
}
