package cn.zgnhit.unityfeast.test;

import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.entity.Rat;
import cn.zgnhit.unityfeast.entity.RatEatWheatGoal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-side checks for native animal goals and the rat's crop interaction. */
public final class RatLifeTests {
    private RatLifeTests() {}

    public static void breeding(GameTestHelper h) {
        var level = h.getLevel();
        var origin = h.absolutePos(new BlockPos(0, 3, 0));
        var fixture = new Fixture(h, origin);
        fixture.floor(-2, 5, -2, 5);
        Rat first = fixture.spawn(.5, .5);
        Rat second = fixture.spawn(1.5, .5);
        first.setNoAi(true);
        second.setNoAi(true);
        var player = fixture.track(UpdateGameTests.player(h));
        var seeds = new ItemStack(Items.WHEAT_SEEDS, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, seeds);
        first.mobInteract(player, InteractionHand.MAIN_HAND);
        h.assertTrue(!first.isInLove() && seeds.getCount() == 2, "wheat seeds do not feed or breed rats");

        var wheat = new ItemStack(Items.WHEAT, 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, wheat);
        h.assertTrue(first.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(), "survival wheat interaction accepted");
        h.assertTrue(first.isInLove() && wheat.getCount() == 3, "first parent consumes exactly one wheat");
        first.mobInteract(player, InteractionHand.MAIN_HAND);
        h.assertValueEqual(wheat.getCount(), 3, "already loving parent does not consume wheat again");
        h.assertTrue(second.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(), "second wheat interaction accepted");
        h.assertTrue(second.isInLove() && wheat.getCount() == 2, "second parent consumes exactly one wheat");

        // Run the real vanilla goal, rather than calling the offspring factory directly.
        var breed = new BreedGoal(first, 1.0);
        h.assertTrue(breed.canUse(), "native breeding goal selects the fed partner");
        var birthArea = new AABB(origin).inflate(5);
        var beforeBirth = level.getEntitiesOfClass(Entity.class, birthArea);
        try {
            breed.start();
            for (int tick = 0; tick < 60 && breed.canContinueToUse(); tick++) breed.tick();
        } finally {
            breed.stop();
            level.getEntitiesOfClass(Entity.class, birthArea).stream()
                    .filter(entity -> !beforeBirth.contains(entity)).forEach(fixture::track);
        }
        var rats = level.getEntitiesOfClass(Rat.class, new AABB(origin).inflate(5));
        h.assertValueEqual(rats.size(), 3, "one pair produces exactly one offspring");
        var child = rats.stream().filter(rat -> rat != first && rat != second).findFirst().orElseThrow();
        h.assertTrue(!child.isBaby() && child.getAge() == 0, "offspring is born adult");
        h.assertValueEqual(child.getBbWidth(), first.getBbWidth(), "adult offspring collision width");
        h.assertValueEqual(child.getBbHeight(), first.getBbHeight(), "adult offspring collision height");
        h.assertTrue(first.getAge() == 6000 && second.getAge() == 6000, "both parents retain native 6000 tick breeding cooldown");
        h.assertTrue(!first.isInLove() && !second.isInLove(), "both parent love states reset");
        first.mobInteract(player, InteractionHand.MAIN_HAND);
        h.assertTrue(wheat.getCount() == 2 && !first.isInLove(), "cooldown blocks feeding and immediate repeat breeding");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        child.saveWithoutId(saved);
        var tag = saved.buildResult();
        tag.putInt("Age", -24000);
        Rat loaded = UnityFeastMod.RAT.get().create(level, EntitySpawnReason.LOAD);
        h.assertTrue(loaded != null, "rat can be created for load");
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        h.assertTrue(!loaded.isBaby() && loaded.getAge() == 0, "negative saved age cannot introduce a baby rat");
        loaded.setBaby(true);
        h.assertTrue(!loaded.isBaby() && loaded.getBbHeight() == first.getBbHeight(), "native setBaby path also keeps ordinary rat size");
        System.out.println("V110 PASS rat wheat feeding, native BreedGoal, one adult offspring, parent cooldown 6000 and negative-age load");
        fixture.close();
        h.succeed();
    }

    public static void cropRules(GameTestHelper h) {
        var level = h.getLevel();
        var origin = h.absolutePos(new BlockPos(0, 3, 0));
        var fixture = new Fixture(h, origin);
        fixture.floor(-2, 19, -4, 4);
        level.getGameRules().set(GameRules.RANDOM_TICK_SPEED, 0, level.getServer());
        level.getGameRules().set(GameRules.MOB_GRIEFING, true, level.getServer());
        Rat rat = fixture.spawn(.5, .5);
        rat.setNoAi(true);
        for (int age = 0; age <= 7; age++) {
            fixture.plant(origin, age);
            rat.setPos(origin.getX() + .5, origin.getY() - .0625, origin.getZ() + .5);
            var eat = new RatEatWheatGoal(rat, 1.0);
            h.assertTrue(eat.canUse(), "planted wheat age " + age + " is a valid target");
            eat.start();
            for (int tick = 0; tick < 80 && level.getBlockState(origin).is(Blocks.WHEAT); tick++) eat.tick();
            eat.stop();
            h.assertTrue(level.getBlockState(origin).isAir(), "wheat age " + age + " disappears after eating");
            h.assertTrue(level.getBlockState(origin.below()).is(Blocks.DIRT), "eaten crop farmland turns to dirt");
            h.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(origin).inflate(2)).isEmpty(), "crop eating emits no wheat or seed drops");
            h.assertTrue(!rat.isInLove(), "eating planted wheat does not automatically enter love mode");
        }

        rat.setPos(origin.getX() + .5, origin.getY(), origin.getZ() + .5);
        rat.setOnGround(true);
        BlockPos fifteen = origin.offset(15, 0, 0);
        fixture.plant(fifteen, 0);
        var atLimit = new RatEatWheatGoal(rat, 1.0);
        h.assertTrue(atLimit.canUse(), "wheat at exactly fifteen blocks is found");
        atLimit.start();
        atLimit.tick();
        h.assertTrue(level.getBlockState(fifteen).is(Blocks.WHEAT), "finding a distant crop does not eat it remotely");
        atLimit.stop();
        fixture.set(fifteen, Blocks.AIR.defaultBlockState());
        BlockPos sixteen = origin.offset(16, 0, 0);
        fixture.plant(sixteen, 7);
        h.assertTrue(!new RatEatWheatGoal(rat, 1.0).canUse(), "wheat sixteen blocks away is outside search radius");
        fixture.set(sixteen, Blocks.AIR.defaultBlockState());

        fixture.plant(origin, 3);
        level.getGameRules().set(GameRules.MOB_GRIEFING, false, level.getServer());
        var protectedCrop = new RatEatWheatGoal(rat, 1.0);
        h.assertTrue(!protectedCrop.canUse(), "mob_griefing=false prevents crop goal");
        h.assertTrue(level.getBlockState(origin).is(Blocks.WHEAT) && level.getBlockState(origin.below()).is(Blocks.FARMLAND), "protected crop and farmland stay intact");
        level.getGameRules().set(GameRules.MOB_GRIEFING, true, level.getServer());
        fixture.set(origin, Blocks.AIR.defaultBlockState());

        BlockPos enclosed = origin.offset(4, 0, 0);
        fixture.plant(enclosed, 4);
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = 0; y <= 2; y++)
            if (x != 0 || z != 0 || y == 2) fixture.set(enclosed.offset(x, y, z), Blocks.STONE.defaultBlockState());
        var blocked = new RatEatWheatGoal(rat, 1.0);
        boolean selected = blocked.canUse();
        if (selected) {
            blocked.start();
            for (int tick = 0; tick < 80; tick++) blocked.tick();
            blocked.stop();
        }
        h.assertTrue(level.getBlockState(enclosed).is(Blocks.WHEAT), "a crop enclosed by solid blocks cannot be eaten through the wall");
        System.out.println("V110 PASS rat all wheat ages 0..7, no drops, dirt conversion, radius 15/16, no distant/wall bite and mob_griefing");
        fixture.close();
        h.succeed();
    }

    public static void forageNavigation(GameTestHelper h) {
        var level = h.getLevel();
        var origin = h.absolutePos(new BlockPos(0, 3, 0));
        var fixture = new Fixture(h, origin);
        fixture.floor(-3, 12, -4, 4);
        level.getGameRules().set(GameRules.RANDOM_TICK_SPEED, 0, level.getServer());
        level.getGameRules().set(GameRules.MOB_GRIEFING, true, level.getServer());
        BlockPos crop = origin.offset(8, 0, 3);
        fixture.plant(crop, 0);
        Rat rat = fixture.spawn(.5, .5);
        Vec3 start = rat.position();
        final int[] eatenAt = {-1};
        for (int tick = 1; tick <= 240; tick++) {
            final int sample = tick;
            h.runAtTickTime(tick, () -> {
                rat.tick();
                if (eatenAt[0] < 0 && level.getBlockState(crop).isAir()) {
                    h.assertTrue(rat.position().distanceToSqr(Vec3.atBottomCenterOf(crop)) < 1,
                            "registered crop goal navigates to wheat before consuming it");
                    eatenAt[0] = sample;
                }
            });
        }
        h.runAtTickTime(245, () -> {
            h.assertTrue(eatenAt[0] > 0, "registered rat goal reached and ate the wheat");
            h.assertTrue(rat.position().distanceToSqr(start) > 1, "rat actually moved under its own AI");
            h.assertTrue(level.getBlockState(crop.below()).is(Blocks.DIRT), "actual navigation ends with dirt conversion");
            h.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(crop).inflate(2)).isEmpty(), "actual foraging has no crop drops");
            System.out.println("V110 PASS rat registered-goal navigation to wheat at 8x3 offset, eaten tick=" + eatenAt[0]);
            fixture.close();
            h.succeed();
        });
    }

    public static void idleRoaming(GameTestHelper h) {
        var origin = h.absolutePos(new BlockPos(0, 3, 0));
        var fixture = new Fixture(h, origin);
        fixture.floor(-14, 14, -14, 14);
        Rat rat = fixture.spawn(.5, .5);
        rat.getRandom().setSeed(261278);
        var observer = fixture.track(UpdateGameTests.player(h));
        observer.setPos(origin.getX() + 2.5, origin.getY(), origin.getZ() + 2.5);
        observer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        observer.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        Vec3 start = rat.position();
        final double[] maxDistanceSquared = {0};
        for (int tick = 1; tick <= 400; tick++) h.runAtTickTime(tick, () -> {
            // Match the normal mob lifecycle; the nearby empty-handed observer is not a movement target.
            rat.checkDespawn();
            rat.tick();
            h.assertTrue(rat.getTarget() == null && !rat.isInLove(), "idle rat remains neutral and out of love mode");
            double dx = rat.getX() - start.x, dz = rat.getZ() - start.z;
            maxDistanceSquared[0] = Math.max(maxDistanceSquared[0], dx * dx + dz * dz);
        });
        h.runAtTickTime(405, () -> {
            h.assertTrue(maxDistanceSquared[0] > 1, "registered idle stroll goal moves an unprovoked rat without crops or held food");
            System.out.println("V110 PASS rat autonomous idle stroll; maximum horizontal distance=" + Math.sqrt(maxDistanceSquared[0]));
            fixture.close();
            h.succeed();
        });
    }

    /** Wide arenas stay in the active test area and undo their own terrain/entity changes. */
    private static final class Fixture implements GameTestListener, AutoCloseable {
        private record SavedBlock(BlockState state, CompoundTag blockEntity) {}
        private final GameTestHelper helper;
        private final ServerLevel level;
        private final BlockPos origin;
        private final Map<BlockPos, SavedBlock> originalBlocks = new LinkedHashMap<>();
        private final List<Entity> createdEntities = new ArrayList<>();
        private final boolean originalGriefing;
        private final int originalRandomTickSpeed;
        private boolean closed;

        Fixture(GameTestHelper helper, BlockPos origin) {
            this.helper = helper;
            this.level = helper.getLevel();
            this.origin = origin;
            originalGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
            originalRandomTickSpeed = level.getGameRules().get(GameRules.RANDOM_TICK_SPEED);
            helper.testInfo.addListener(this);
        }

        <T extends Entity> T track(T entity) {
            createdEntities.add(entity);
            return entity;
        }

        Rat spawn(double x, double z) {
            Rat rat = UnityFeastMod.RAT.get().create(level, EntitySpawnReason.COMMAND);
            helper.assertTrue(rat != null, "create rat");
            track(rat);
            rat.setPos(origin.getX() + x, origin.getY(), origin.getZ() + z);
            rat.setOnGround(true);
            level.addFreshEntity(rat);
            return rat;
        }

        void set(BlockPos pos, BlockState state) {
            originalBlocks.computeIfAbsent(pos.immutable(), key -> {
                var blockEntity = level.getBlockEntity(key);
                return new SavedBlock(level.getBlockState(key), blockEntity == null ? null : blockEntity.saveWithFullMetadata(level.registryAccess()));
            });
            level.setBlock(pos, state, 3);
        }

        void plant(BlockPos pos, int age) {
            set(pos.below(), Blocks.FARMLAND.defaultBlockState());
            set(pos, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, age));
        }

        void floor(int minX, int maxX, int minZ, int maxZ) {
            for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
                set(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y <= 3; y++) set(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
        }

        @Override public void close() {
            if (closed) return;
            closed = true;
            for (Entity entity : createdEntities) {
                if (entity instanceof ServerPlayer player) level.getServer().getPlayerList().remove(player);
                entity.discard();
            }
            for (var entry : originalBlocks.entrySet()) {
                // Restore exact saved states without neighbor updates creating fixture drops.
                level.setBlock(entry.getKey(), entry.getValue().state(), 18);
                if (entry.getValue().blockEntity() != null && level.getBlockEntity(entry.getKey()) != null)
                    level.getBlockEntity(entry.getKey()).loadWithComponents(TagValueInput.create(
                            ProblemReporter.DISCARDING, level.registryAccess(), entry.getValue().blockEntity()));
            }
            level.getGameRules().set(GameRules.MOB_GRIEFING, originalGriefing, level.getServer());
            level.getGameRules().set(GameRules.RANDOM_TICK_SPEED, originalRandomTickSpeed, level.getServer());
        }

        @Override public void testStructureLoaded(GameTestInfo testInfo) {}
        @Override public void testPassed(GameTestInfo testInfo, GameTestRunner runner) { close(); }
        @Override public void testFailed(GameTestInfo testInfo, GameTestRunner runner) { close(); }
        @Override public void testAddedForRerun(GameTestInfo original, GameTestInfo copy, GameTestRunner runner) { close(); }
    }
}
