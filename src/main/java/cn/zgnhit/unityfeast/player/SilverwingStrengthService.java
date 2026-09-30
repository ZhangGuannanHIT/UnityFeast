package cn.zgnhit.unityfeast.player;

import cn.zgnhit.unityfeast.UnityFeastMod;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Maintains one vanilla Strength instance, retaining external effects independently of the suit's infinite I. */
public final class SilverwingStrengthService {
    private SilverwingStrengthService() {}
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, UnityFeastMod.ID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SilverwingStrengthData>> DATA =
            ATTACHMENTS.register("silverwing_strength", () -> AttachmentType.builder(SilverwingStrengthData::new)
                    .serialize(SilverwingStrengthData.SERIALIZER).build());

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
        NeoForge.EVENT_BUS.register(SilverwingStrengthService.class);
    }

    public static boolean isComplete(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(UnityFeastMod.SILVERWING_HELMET)
                && player.getItemBySlot(EquipmentSlot.CHEST).is(UnityFeastMod.SILVERWING_CHESTPLATE)
                && player.getItemBySlot(EquipmentSlot.LEGS).is(UnityFeastMod.SILVERWING_LEGGINGS)
                && player.getItemBySlot(EquipmentSlot.FEET).is(UnityFeastMod.SILVERWING_BOOTS);
    }

    /** The vanilla copy constructor omits hidden effects; its codec retains the complete chain. */
    private static MobEffectInstance copy(ServerPlayer player, MobEffectInstance effect) {
        if (effect == null) return null;
        var ops = player.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        return MobEffectInstance.CODEC.parse(ops, MobEffectInstance.CODEC.encodeStart(ops, effect).getOrThrow()).getOrThrow();
    }

    private static MobEffectInstance suitEffect() {
        return new MobEffectInstance(MobEffects.STRENGTH, MobEffectInstance.INFINITE_DURATION, 0, true, false, true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void added(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getEffectInstance().is(MobEffects.STRENGTH)) return;
        if (!player.hasData(DATA)) return;
        var data = player.getData(DATA);
        if (data.internal || !data.managed) return;
        data.settleRemoval();
        var incoming = copy(player, event.getEffectInstance());
        if (data.external == null) data.external = incoming;
        else data.external.update(incoming);
        data.dirty = true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void removed(MobEffectEvent.Remove event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getEffect().equals(MobEffects.STRENGTH)
                || !player.hasData(DATA)) return;
        var data = player.getData(DATA);
        if (data.managed && !data.internal) data.pendingRemoval = event;
    }

    @SubscribeEvent
    public static void beforeTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.hasData(DATA)) return;
        var data = player.getData(DATA);
        data.settleRemoval();
        if (data.managed && data.external != null && player.isAlive()) {
            // Strength has no periodic gameplay action. This advances the complete vanilla hidden chain exactly once,
            // before LivingEntity ticks the displayed copy, without applying another attribute modifier or damage tick.
            data.external.tickClient();
            if (!data.external.isInfiniteDuration() && data.external.getDuration() <= 0) data.external = null;
        }
    }

    @SubscribeEvent public static void afterTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) reconcile(player);
    }

    /** Calibrate from actual equipment; called at most one server player tick after an equipment mutation. */
    public static void reconcile(ServerPlayer player) {
        boolean complete = player.isAlive() && isComplete(player);
        if (!complete && !player.hasData(DATA)) return;
        var data = player.getData(DATA);
        data.settleRemoval();
        if (!player.isAlive() && data.managed) data.external = null;
        if (!data.managed) {
            if (!complete) return;
            data.external = copy(player, player.getEffect(MobEffects.STRENGTH));
            data.managed = true;
            data.dirty = true;
        }
        if (!complete) {
            replace(player, data, data.external);
            data.managed = false;
            data.external = null;
            data.dirty = false;
            return;
        }

        var current = player.getEffect(MobEffects.STRENGTH);
        boolean stronger = data.external != null && data.external.getAmplifier() > 0;
        boolean correct = current != null && current.getAmplifier() == (stronger ? data.external.getAmplifier() : 0)
                && (stronger ? current.getDuration() == data.external.getDuration() : current.isInfiniteDuration());
        if (data.dirty || !correct) {
            var combined = stronger ? copy(player, data.external) : suitEffect();
            if (stronger) combined.update(suitEffect());
            replace(player, data, combined);
            data.dirty = false;
        }
    }

    private static void replace(ServerPlayer player, SilverwingStrengthData data, MobEffectInstance desired) {
        data.internal = true;
        try {
            if (desired == null) player.removeEffect(MobEffects.STRENGTH);
            else player.forceAddEffect(copy(player, desired), player);
        } finally { data.internal = false; }
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) reconcile(player);
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) reconcile(player);
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) reconcile(player);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void clone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Death never copies temporary effects, even when keepInventory retains the armor.
        if (event.isWasDeath()) {
            player.setData(DATA, new SilverwingStrengthData());
        } else if (event.getOriginal() instanceof ServerPlayer oldPlayer && oldPlayer.hasData(DATA)) {
            var old = oldPlayer.getData(DATA);
            old.settleRemoval();
            var replacement = new SilverwingStrengthData();
            replacement.managed = old.managed;
            replacement.external = copy(player, old.external);
            replacement.dirty = old.managed;
            player.setData(DATA, replacement);
        }
        reconcile(player);
    }
}
