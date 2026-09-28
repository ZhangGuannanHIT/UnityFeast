package cn.zgnhit.unityfeast.player;

import cn.zgnhit.unityfeast.UnityFeastMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class PlayerLifecycleEvents {
    private PlayerLifecycleEvents() {}
    // Capture the rule now, but do not clear anything inside this cancellable event.
    // Retain the event until tick/clone/save so a later listener's cancellation is honored.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var old = player.getData(UnityFeastMod.HEART_DATA);
            boolean keep = player.level().getGameRules().get(GameRules.KEEP_INVENTORY);
            player.setData(UnityFeastMod.HEART_DATA, new UnityHeartData(old.hearts(),old.soups(), keep ? 2 : 1, event));
        }
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.hasData(UnityFeastMod.HEART_DATA)) {
            var data = player.getData(UnityFeastMod.HEART_DATA);
            if (data.pendingDeath() != null) {
                int policy = data.confirmedPolicy();
                player.setData(UnityFeastMod.HEART_DATA, new UnityHeartData(policy == 1 ? 0 : data.hearts(),policy == 1 ? 0 : data.soups(), policy));
                UnityHeartService.reconcile(player);
            }
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void clone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var old = event.getOriginal().getData(UnityFeastMod.HEART_DATA);
            player.setData(UnityFeastMod.HEART_DATA, old.afterClone(event.isWasDeath()));
            UnityHeartService.reconcile(player);
        }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) UnityHeartService.reconcile(player);
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) UnityHeartService.reconcile(player);
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) UnityHeartService.reconcile(player);
    }
}
