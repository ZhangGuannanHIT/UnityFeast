package cn.zgnhit.unityfeast.client;

import cn.zgnhit.unityfeast.UnityFeastMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid=UnityFeastMod.ID,value=Dist.CLIENT)
public final class ClientRegistration {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(UnityFeastMod.TABLE_ENTITY.get(),TableRenderer::new);
        event.registerEntityRenderer(UnityFeastMod.RAT.get(),RatRenderer::new);
    }
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event){event.registerLayerDefinition(RatModel.LAYER,RatModel::layer);}
}
