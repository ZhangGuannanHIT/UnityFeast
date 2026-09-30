package cn.zgnhit.unityfeast.client;

import cn.zgnhit.unityfeast.entity.BottleCapProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

/** The cap's ordinary three-dimensional item model tumbles in world space, not as a billboard. */
public final class BottleCapRenderer extends EntityRenderer<BottleCapProjectile, ThrownItemRenderState> {
    private final ItemModelResolver items;
    public BottleCapRenderer(EntityRendererProvider.Context context) { super(context); items = context.getItemModelResolver(); }
    @Override public ThrownItemRenderState createRenderState() { return new ThrownItemRenderState(); }
    @Override public void extractRenderState(BottleCapProjectile entity, ThrownItemRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        items.updateForNonLiving(state.item, entity.getItem(), ItemDisplayContext.FIXED, entity);
    }
    @Override public void submit(ThrownItemRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.scale(.6F, .6F, .6F);
        pose.mulPose(Axis.YP.rotationDegrees(state.ageInTicks * 30F));
        pose.mulPose(Axis.XP.rotationDegrees(state.ageInTicks * 18F));
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
