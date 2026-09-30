package cn.zgnhit.unityfeast.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import org.jspecify.annotations.Nullable;

/** A passive, adult-only animal; deliberately shares no rat or rideable-pig behavior. */
public final class GreenBottle extends Animal {
    public GreenBottle(EntityType<? extends GreenBottle> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder attributes() {
        // Pig's published 26.1 movement attribute, with only the requested health changed.
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, .25);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.25));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Native navigation, MoveControl and JumpControl handle a full block by jumping.
    }

    public static boolean canSpawn(EntityType<GreenBottle> type, ServerLevelAccessor level,
                                   EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        // This is precisely Animal's non-light predicate. ON_GROUND and NaturalSpawner
        // retain their own liquid, space, distance, category-cap and scheduling checks.
        return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON);
    }

    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public boolean canFallInLove() { return false; }
    @Override public boolean canMate(Animal other) { return false; }
    @Override public void setInLove(@Nullable Player player) { }
    @Override public void setInLoveTime(int ticks) { super.setInLoveTime(0); }
    @Override public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }
    @Override public void setAge(int age) { super.setAge(0); }
    @Override public boolean canUseSlot(EquipmentSlot slot) { return slot != EquipmentSlot.SADDLE && super.canUseSlot(slot); }
    @Override public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                           EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        // Disable the group baby lottery itself, as well as rejecting negative ages.
        return super.finalizeSpawn(level, difficulty, reason,
                data == null ? new AgeableMob.AgeableMobGroupData(false) : data);
    }
    @Override protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        resetLove();
        setAge(0);
    }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.GLASS_HIT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.GLASS_BREAK; }
}
