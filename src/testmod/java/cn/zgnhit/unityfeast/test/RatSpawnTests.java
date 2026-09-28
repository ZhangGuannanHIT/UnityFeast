package cn.zgnhit.unityfeast.test;

import cn.zgnhit.unityfeast.UnityFeastMod;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;

public final class RatSpawnTests {
    private RatSpawnTests() {}

    public static void pigParity(GameTestHelper h) {
        var level = h.getLevel();
        var rat = UnityFeastMod.RAT.get();
        h.assertTrue(rat.getCategory() == MobCategory.CREATURE, "rats share the normal animal category cap");
        h.assertTrue(SpawnPlacements.getPlacementType(rat) == SpawnPlacements.getPlacementType(EntityType.PIG), "pig ground placement");
        h.assertTrue(SpawnPlacements.getHeightmapType(rat) == SpawnPlacements.getHeightmapType(EntityType.PIG), "pig heightmap");
        var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        int matchingBiomes = 0;
        for (var biome : biomes.listElements().toList()) {
            var entries = biome.value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap();
            var pigs = entries.stream().filter(e -> e.value().type() == EntityType.PIG)
                    .map(e -> List.of(e.weight(), e.value().minCount(), e.value().maxCount())).toList();
            var rats = entries.stream().filter(e -> e.value().type() == rat)
                    .map(e -> List.of(e.weight(), e.value().minCount(), e.value().maxCount())).toList();
            h.assertTrue(pigs.equals(rats), "rat weight, group size and biome coverage match pigs: " + biome.key().identifier());
            if (!pigs.isEmpty()) matchingBiomes++;
        }
        h.assertTrue(matchingBiomes > 0, "rat has real registered natural spawning biomes");
        var plains = biomes.getOrThrow(Biomes.PLAINS);
        h.assertTrue(plains.value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream()
                .anyMatch(e -> e.value().type() == rat && e.weight() == 10 && e.value().minCount() == 4 && e.value().maxCount() == 4),
                "vanilla plains rat weight 10 and group 4");

        // Separate sealed cells avoid changing a light source during accelerated GameTests.
        var brightCell = h.absolutePos(new BlockPos(2, 5, 2));
        var darkCell = h.absolutePos(new BlockPos(8, 5, 2));
        makeLightCell(h, brightCell, 9);
        makeLightCell(h, darkCell, 8);

        // A separate elevated single chunk is used by the actual vanilla chunk-initialization path.
        var chunk = ChunkPos.containing(h.absolutePos(new BlockPos(48, 0, 48)));
        int groundY = brightCell.getY() + 16;
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++)
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                level.setBlock(new BlockPos(x, groundY, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, groundY + 1, z), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, groundY + 2, z), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, groundY + 3, z), Blocks.LIGHT.defaultBlockState(), 3);
            }
        int coveredBiomes = matchingBiomes;
        awaitFixtureLight(h, brightCell, darkCell, 160, () -> {
            checkPlacements(h, brightCell, true);
            checkPlacements(h, darkCell, false);
            level.setBlock(brightCell.below(), Blocks.STONE.defaultBlockState(), 3);
            checkPlacements(h, brightCell, false);
            level.setBlock(brightCell.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            level.getGameRules().set(GameRules.SPAWN_MOBS, true, level.getServer());
            var bounds = new AABB(chunk.getMinBlockX(), groundY, chunk.getMinBlockZ(),
                    chunk.getMaxBlockX() + 1, groundY + 5, chunk.getMaxBlockZ() + 1);
            var random = RandomSource.create(0x261278L);
            int rats = 0, pigs = 0, attempts = 0;
            // Independent generation attempts, not a production replenishment loop. Clear each
            // result so collisions and the test itself cannot accumulate hundreds of entities.
            while ((rats == 0 || pigs == 0) && attempts++ < 1024) {
                NaturalSpawner.spawnMobsForChunkGeneration(level, plains, chunk, random);
                for (var mob : level.getEntitiesOfClass(Mob.class, bounds)) {
                    if (mob.getType() == rat) rats++;
                    if (mob.getType() == EntityType.PIG) pigs++;
                    mob.discard();
                }
            }
            h.assertTrue(rats > 0 && pigs > 0, "vanilla chunk initialization actually generated both rats and pigs");
            System.out.println("V110 PASS rat pig spawn parity: biomes=" + coveredBiomes
                    + ", actual chunk-generation attempts=" + attempts + ", rats=" + rats + ", pigs=" + pigs
                    + "; registered natural/chunk rules: light 9 allowed, 8 rejected, stone rejected");
            h.succeed();
        });
    }

    private static void makeLightCell(GameTestHelper h, BlockPos cell, int light) {
        var level = h.getLevel();
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = -1; y <= 2; y++)
            level.setBlock(cell.offset(x, y, z), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(cell.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        level.setBlock(cell.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(cell, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, light), 3);
    }

    private static void awaitFixtureLight(GameTestHelper h, BlockPos bright, BlockPos dark, int remaining, Runnable ready) {
        h.runAfterDelay(1, () -> {
            int brightValue = h.getLevel().getRawBrightness(bright, 0);
            int darkValue = h.getLevel().getRawBrightness(dark, 0);
            if (brightValue == 9 && darkValue == 8) {
                ready.run(); // The actual chunk-generation experiment runs only once.
            } else {
                h.assertTrue(remaining > 0, "fixture light did not settle: bright=" + brightValue + ", dark=" + darkValue);
                awaitFixtureLight(h, bright, dark, remaining - 1, ready);
            }
        });
    }

    private static void checkPlacements(GameTestHelper h, BlockPos pos, boolean expected) {
        for (var reason : List.of(EntitySpawnReason.NATURAL, EntitySpawnReason.CHUNK_GENERATION)) {
            boolean pig = SpawnPlacements.checkSpawnRules(EntityType.PIG, h.getLevel(), reason, pos, RandomSource.create(1));
            boolean rat = SpawnPlacements.checkSpawnRules(UnityFeastMod.RAT.get(), h.getLevel(), reason, pos, RandomSource.create(1));
            h.assertTrue(pig == expected && rat == pig, "registered rat/pig spawn predicate parity: " + reason);
        }
    }
}
