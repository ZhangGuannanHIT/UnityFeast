package cn.zgnhit.unityfeast.combat;

import cn.zgnhit.unityfeast.entity.BottleCapProjectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public final class BottleCapLaunch {
    private BottleCapLaunch() {}
    /** Exactly the local snowball's power 1.5 and inaccuracy 1; no weapon enchantment spawn callback. */
    public static boolean launch(ServerLevel level, Player player) {
        var cap = new BottleCapProjectile(level, player);
        cap.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.5F, 1F);
        if (!level.addFreshEntity(cap)) return false;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_PLACE,
                SoundSource.PLAYERS, .45F, 1.4F + level.getRandom().nextFloat() * .2F);
        return true;
    }
}
