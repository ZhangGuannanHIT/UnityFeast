package cn.zgnhit.unityfeast.entity;

import cn.zgnhit.unityfeast.UnityFeastMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.AABB;

public final class Rat extends Animal {
    private static final EntityDataAccessor<Boolean> RED_EYES=SynchedEntityData.defineId(Rat.class,EntityDataSerializers.BOOLEAN);
    private long nextAttackTick;
    private long lastAttackTick=Long.MIN_VALUE;

    public Rat(EntityType<? extends Rat> type,Level level) { super(type,level); }
    public static AttributeSupplier.Builder attributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH,10).add(Attributes.ATTACK_DAMAGE,1)
                // Rabbit pauses between hops; a continuous four-foot gait needs a lower base speed.
                .add(Attributes.MOVEMENT_SPEED,.22).add(Attributes.FOLLOW_RANGE,16);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder);builder.define(RED_EYES,false); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new MeleeAttackGoal(this,1.4,true));
        goalSelector.addGoal(2,new BreedGoal(this,1.0));
        goalSelector.addGoal(3,new TemptGoal(this,1.2,this::isFood,false));
        goalSelector.addGoal(4,new RatEatWheatGoal(this,1.0));
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,1.0));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,6));
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this)); // deliberately no alertOthers or nearest-player goal
    }
    public static int localLight(LevelReader level,BlockPos pos) { return level.getMaxLocalRawBrightness(pos); }
    public static boolean redAt(int light,boolean retaliation) { return light<7||retaliation; }
    public static boolean canSpawn(EntityType<Rat> type,ServerLevelAccessor level,EntitySpawnReason reason,BlockPos pos,RandomSource random) {
        return Animal.checkAnimalSpawnRules(type,level,reason,pos,random);
    }
    public boolean redEyes() { return entityData.get(RED_EYES); }
    private boolean validTarget(LivingEntity target) {
        return target!=null&&target!=this&&target.isAlive()&&target.level()==level()&&canAttack(target)
                && !(target instanceof Player p&&(p.isCreative()||p.isSpectator()))
                && distanceToSqr(target)<=getAttributeValue(Attributes.FOLLOW_RANGE)*getAttributeValue(Attributes.FOLLOW_RANGE);
    }
    public void updateEyes() {
        if(!level().isClientSide()) {
            boolean red=redAt(localLight(level(),blockPosition()),validTarget(getTarget()));
            if(red!=redEyes()) entityData.set(RED_EYES,red);
        }
    }
    @Override public void setTarget(LivingEntity target) { super.setTarget(target); updateEyes(); }
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount) {
        boolean hurt=super.hurtServer(level,source,amount);
        if(hurt&&isAlive()&&source.getEntity() instanceof LivingEntity attacker&&validTarget(attacker)) setTarget(attacker);
        return hurt;
    }
    @Override public void tick() {
        super.tick();
        if(!level().isClientSide()) {
            if(getTarget()!=null&&!validTarget(getTarget())) setTarget(null);
            if(tickCount==1||tickCount%5==0) updateEyes();
        }
    }
    @Override public boolean isWithinMeleeAttackRange(LivingEntity target) {
        AABB own=getBoundingBox(),other=target.getBoundingBox();
        // Include touching faces, but never expand either collision box for extra reach.
        return own.minX<=other.maxX&&own.maxX>=other.minX
                &&own.minY<=other.maxY&&own.maxY>=other.minY
                &&own.minZ<=other.maxZ&&own.maxZ>=other.minZ;
    }
    @Override public boolean doHurtTarget(ServerLevel level,Entity target) {
        if(!(target instanceof LivingEntity living)||!validTarget(living)||!isWithinMeleeAttackRange(living)||!hasLineOfSight(living)) return false;
        long now=level.getGameTime();if(now<nextAttackTick) return false;
        nextAttackTick=now+20;lastAttackTick=now; // entity-owned deadline survives AI/target restarts
        return super.doHurtTarget(level,target);
    }
    public long lastAttackTick() { return lastAttackTick; }
    @Override public boolean isFood(ItemStack stack) { return stack.is(Items.WHEAT); }
    @Override public AgeableMob getBreedOffspring(ServerLevel level,AgeableMob other) {
        return UnityFeastMod.RAT.get().create(level,EntitySpawnReason.BREEDING);
    }
    @Override public void setAge(int age) {
        // All rats have adult size; keep the vanilla positive breeding cooldown intact.
        super.setAge(Math.max(0,age));
    }
    @Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out);out.putLong("next_attack_tick",nextAttackTick); }
    @Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in);nextAttackTick=in.getLongOr("next_attack_tick",0); }
}
