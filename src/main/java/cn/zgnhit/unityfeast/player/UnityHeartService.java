package cn.zgnhit.unityfeast.player;

import cn.zgnhit.unityfeast.UnityFeastMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.TagValueInput;

public final class UnityHeartService {
    public static final Identifier BONUS = UnityFeastMod.id("unity_heart_bonus");
    private UnityHeartService() {}
    public static long next(long count, double attributeMaximum) {
        // Bound storage by the actual legal attribute range, not an extra gameplay cap.
        long max = (long)Math.min(Long.MAX_VALUE / 2L, Math.ceil(attributeMaximum / 2.0));
        return Math.min(Math.max(0, count), max) < max ? Math.min(Math.max(0, count), max) + 1 : max;
    }
    public static void consume(ServerPlayer player) {
        double max = ((RangedAttribute)Attributes.MAX_HEALTH.value()).getMaxValue();
        var data = new UnityHeartData(next(player.getData(UnityFeastMod.HEART_DATA).effectiveHearts(), max), 0);
        player.setData(UnityFeastMod.HEART_DATA, data);
        apply(player, data.hearts());
        player.setHealth(player.getMaxHealth());
        // FoodData's own NBT reader resets all four fields without reflection or an AT.
        CompoundTag full = new CompoundTag();
        full.putInt("foodLevel", 20); full.putFloat("foodSaturationLevel", 20);
        full.putFloat("foodExhaustionLevel", 0); full.putInt("foodTickTimer", 0);
        player.getFoodData().readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), full));
    }
    public static void reconcile(Player player) { apply(player, player.getData(UnityFeastMod.HEART_DATA).effectiveHearts()); }
    public static void apply(Player player, long hearts) {
        AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;
        double limit = ((RangedAttribute)Attributes.MAX_HEALTH.value()).getMaxValue();
        double bonus = Math.min(limit, Math.max(0, hearts) * 2.0);
        var existing = attribute.getModifier(BONUS);
        if (existing != null && existing.amount() == bonus && existing.operation() == AttributeModifier.Operation.ADD_VALUE) return;
        attribute.removeModifier(BONUS);
        if (bonus > 0) attribute.addPermanentModifier(new AttributeModifier(BONUS, bonus, AttributeModifier.Operation.ADD_VALUE));
    }
}
