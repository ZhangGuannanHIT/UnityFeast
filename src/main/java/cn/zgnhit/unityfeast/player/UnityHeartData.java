package cn.zgnhit.unityfeast.player;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.*;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** deathPolicy: 0 = alive, 1 = confirmed death/clear, 2 = confirmed death/keep. */
public record UnityHeartData(long hearts, long soups, int deathPolicy, LivingDeathEvent pendingDeath) {
    public static final UnityHeartData EMPTY = new UnityHeartData(0, 0);
    public UnityHeartData(long hearts, int deathPolicy) { this(hearts, 0, deathPolicy, null); }
    public UnityHeartData(long hearts, int deathPolicy, LivingDeathEvent pending) { this(hearts, 0, deathPolicy, pending); }
    public UnityHeartData(long hearts, long soups, int deathPolicy) { this(hearts, soups, deathPolicy, null); }
    public UnityHeartData {
        hearts = Math.max(0, hearts);
        soups = Math.max(0, soups);
        if (deathPolicy < 0 || deathPolicy > 2) deathPolicy = 0;
    }
    public int confirmedPolicy() { return pendingDeath != null && pendingDeath.isCanceled() ? 0 : deathPolicy; }
    public long effectiveHearts() { return confirmedPolicy() == 1 ? 0 : hearts; }
    public long effectiveSoups() { return confirmedPolicy() == 1 ? 0 : soups; }
    public double effectiveBonus() { return effectiveHearts()*2.0+effectiveSoups()*20.0; }
    public UnityHeartData afterClone(boolean death) { return new UnityHeartData(death ? effectiveHearts() : hearts, death ? effectiveSoups() : soups, 0); }
    public static final IAttachmentSerializer<UnityHeartData> SERIALIZER = new IAttachmentSerializer<>() {
        @Override public UnityHeartData read(IAttachmentHolder holder, ValueInput input) {
            var data = new UnityHeartData(input.getLongOr("hearts", 0), input.getLongOr("weijixian_count",0), input.getIntOr("death_policy", 0));
            // Entity reads attachments BEFORE LivingEntity reads attributes and Health.
            // Install before setHealth can clamp a saved health > 20; the permanent modifier
            // is also saved in vanilla attributes and subsequently reconciled from this count.
            if (holder instanceof Player player && !player.level().isClientSide()) UnityHeartService.applyBonus(player, data.effectiveBonus());
            return data;
        }
        @Override public boolean write(UnityHeartData data, ValueOutput output) {
            output.putLong("hearts", data.hearts);
            output.putLong("weijixian_count",data.soups);
            output.putInt("death_policy", data.confirmedPolicy());
            return true;
        }
    };
}
