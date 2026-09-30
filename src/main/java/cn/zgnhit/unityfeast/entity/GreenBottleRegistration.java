package cn.zgnhit.unityfeast.entity;

import cn.zgnhit.unityfeast.UnityFeastMod;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = UnityFeastMod.ID)
public final class GreenBottleRegistration {
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) {
        event.put(UnityFeastMod.GREEN_BOTTLE.get(), GreenBottle.attributes().build());
    }
    @SubscribeEvent public static void placements(RegisterSpawnPlacementsEvent event) {
        event.register(UnityFeastMod.GREEN_BOTTLE.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, GreenBottle::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
