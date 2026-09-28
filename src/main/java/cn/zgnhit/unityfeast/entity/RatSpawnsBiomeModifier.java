package cn.zgnhit.unityfeast.entity;

import cn.zgnhit.unityfeast.UnityFeastMod;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

public record RatSpawnsBiomeModifier() implements BiomeModifier {
    public static final MapCodec<RatSpawnsBiomeModifier> CODEC=MapCodec.unit(RatSpawnsBiomeModifier::new);

    @Override public void modify(Holder<Biome> biome,Phase phase,ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if(phase!=Phase.AFTER_EVERYTHING) return;
        var spawns=builder.getMobSpawnSettings();
        var entries=spawns.getSpawner(MobCategory.CREATURE).build().unwrap();
        // Respect an explicit rat spawn entry supplied by a datapack or another modifier.
        if(entries.stream().anyMatch(entry->entry.value().type()==UnityFeastMod.RAT.get())) return;
        for(var entry:entries) {
            var pig=entry.value();
            if(pig.type()==EntityType.PIG) {
                // Follow pig biomes and their actual weight/group size (vanilla: 10, 4-4).
                spawns.addSpawn(MobCategory.CREATURE,entry.weight(),
                        new MobSpawnSettings.SpawnerData(UnityFeastMod.RAT.get(),pig.minCount(),pig.maxCount()));
            }
        }
    }

    @Override public MapCodec<? extends BiomeModifier> codec() { return CODEC; }
}
