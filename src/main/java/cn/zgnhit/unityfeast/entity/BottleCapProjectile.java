package cn.zgnhit.unityfeast.entity;

import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.combat.BottleCombat;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

/** Snowball movement without snowball damage, recovery, arrow embedding or pearl side effects. */
public class BottleCapProjectile extends ThrowableItemProjectile {
    public static final int MAX_LIFETIME = 200;
    private int livedTicks;
    private long expiresAt;
    private boolean resolved;
    private LivingEntity hitTarget;
    private boolean effectHit;
    private boolean playerOwned;
    private ProjectileImpactEvent pendingImpact;

    public BottleCapProjectile(EntityType<? extends BottleCapProjectile> type, Level level) {
        super(type, level);
        expiresAt = level.getGameTime() + MAX_LIFETIME;
    }

    public BottleCapProjectile(Level level, LivingEntity owner) {
        this(UnityFeastMod.BOTTLE_CAP_PROJECTILE.get(), level);
        setPos(owner.getX(), owner.getEyeY() - .1, owner.getZ());
        setOwner(owner);
        playerOwned = owner instanceof Player;
    }

    @Override protected Item getDefaultItem() { return UnityFeastMod.BOTTLE_CAP.get(); }
    @Override public ItemStack getWeaponItem() { return ItemStack.EMPTY; }
    @Override public boolean canUsePortal(boolean ignorePassenger) { return false; }

    @Override public void tick() {
        if (!level().isClientSide() && (resolved || ++livedTicks >= MAX_LIFETIME || level().getGameTime() >= expiresAt)) {
            discard();
            return;
        }
        pendingImpact = null;
        super.tick();
        rejectCanceledImpact(); // Earlier 26.1 builds may skip onHit entirely after cancellation.
        pendingImpact = null;
    }

    @Override protected ProjectileDeflection hitTargetOrDeflectSelf(HitResult result) {
        if (rejectCanceledImpact()) return ProjectileDeflection.NONE;
        if (result instanceof EntityHitResult hit && hit.getEntity().deflection(this) != ProjectileDeflection.NONE) {
            if (!level().isClientSide()) finish();
            return ProjectileDeflection.NONE;
        }
        return super.hitTargetOrDeflectSelf(result);
    }

    @Override protected void onHit(HitResult result) {
        if (resolved || rejectCanceledImpact() || level().isClientSide()) return;
        resolved = true; // Before any callback: cancellation or recursive collision cannot retry this cap.
        super.onHit(result);
        finish();
    }

    /** Keep the event itself so later listeners, including later LOWEST listeners, remain authoritative. */
    public void observeImpact(ProjectileImpactEvent event) { pendingImpact = event; }

    private boolean rejectCanceledImpact() {
        if (pendingImpact == null || !pendingImpact.isCanceled()) return false;
        pendingImpact = null;
        resolved = true;
        discard(); // No damage, successful-impact sound, particles, retry or recoverable item.
        return true;
    }

    @Override protected void onHitEntity(EntityHitResult result) {
        if (level() instanceof ServerLevel server && result.getEntity() instanceof LivingEntity target) {
            // An offline/unresolved player owner must not turn a PVP projectile into anonymous damage.
            if (target instanceof Player && playerOwned && !(getOwner() instanceof Player)) return;
            hitTarget = target;
            var type = server.registryAccess().getOrThrow(BottleCombat.BOTTLE_CAP);
            target.hurtServer(server, new DamageSource(type, this, getOwner()), 4F);
            hitTarget = null;
        }
    }

    /** Called only by the successful post-damage hook, which also sees fully absorbed damage. */
    public boolean claimEffectHit(LivingEntity target) {
        if (target != hitTarget || effectHit) return false;
        effectHit = true;
        return true;
    }

    private void finish() {
        resolved = true;
        level().broadcastEntityEvent(this, (byte)3);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_HIT, SoundSource.NEUTRAL, .35F, 1.7F);
        discard();
    }

    @Override public void handleEntityEvent(byte id) {
        if (id == 3) {
            var particle = new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(new ItemStack(getDefaultItem())));
            for (int i = 0; i < 5; i++) level().addParticle(particle, getX(), getY(), getZ(),
                    (random.nextDouble() - .5) * .08, .025, (random.nextDouble() - .5) * .08);
        } else super.handleEntityEvent(id);
    }

    @Override protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("CapAge", livedTicks);
        output.putLong("CapExpiresAt", expiresAt);
        output.putBoolean("CapResolved", resolved);
        output.putBoolean("CapPlayerOwned", playerOwned);
    }

    @Override protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        livedTicks = Math.max(0, input.getIntOr("CapAge", 0));
        expiresAt = input.getLongOr("CapExpiresAt", level().getGameTime() + Math.max(0, MAX_LIFETIME - livedTicks));
        resolved = input.getBooleanOr("CapResolved", false);
        playerOwned = input.getBooleanOr("CapPlayerOwned", getOwner() instanceof Player);
    }
}
