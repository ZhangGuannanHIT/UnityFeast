package cn.zgnhit.unityfeast.player;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/** Only the external Strength chain is saved. The managed flag is cleanup provenance, never equipment eligibility. */
public final class SilverwingStrengthData {
    boolean managed;
    MobEffectInstance external;
    boolean internal;
    boolean dirty;
    MobEffectEvent.Remove pendingRemoval;

    void settleRemoval() {
        if (pendingRemoval != null) {
            if (!pendingRemoval.isCanceled()) { external = null; dirty = true; }
            pendingRemoval = null;
        }
    }

    public static final IAttachmentSerializer<SilverwingStrengthData> SERIALIZER = new IAttachmentSerializer<>() {
        @Override public SilverwingStrengthData read(IAttachmentHolder holder, ValueInput input) {
            var result = new SilverwingStrengthData();
            result.managed = input.getBooleanOr("managed", false);
            result.external = input.read("external_strength", MobEffectInstance.CODEC)
                    .filter(effect -> effect.is(MobEffects.STRENGTH)).orElse(null);
            result.dirty = result.managed;
            return result;
        }
        @Override public boolean write(SilverwingStrengthData data, ValueOutput output) {
            data.settleRemoval();
            output.putBoolean("managed", data.managed);
            if (data.managed && data.external != null)
                output.store("external_strength", MobEffectInstance.CODEC, data.external);
            return data.managed;
        }
    };
}
