package cn.zgnhit.unityfeast.client;

import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.entity.GreenBottle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public final class GreenBottleRenderer extends MobRenderer<GreenBottle, GreenBottleRenderState, GreenBottleModel> {
    private static final Identifier TEXTURE = UnityFeastMod.id("textures/entity/green_bottle.png");
    public GreenBottleRenderer(EntityRendererProvider.Context context) {
        super(context, new GreenBottleModel(), .28F);
        addLayer(new RenderLayer<>(this) {
            @Override public void submit(PoseStack poses, SubmitNodeCollector collector, int light,
                    GreenBottleRenderState state, float yRot, float xRot) {
                submitBottle(poses, collector, light, state);
            }
        });
    }
    // The layer submits the real mesh after vanilla transforms; do not submit the empty root.
    @Override protected RenderType getRenderType(GreenBottleRenderState state, boolean visible,
            boolean transparent, boolean glowing) { return null; }

    private void submitBottle(PoseStack poses, SubmitNodeCollector collector, int light, GreenBottleRenderState state) {
        boolean visible = isBodyVisible(state);
        boolean transparent = !visible && !state.isInvisibleToPlayer;
        int overlay = getOverlayCoords(state, getWhiteOverlayProgress(state));
        if (visible || transparent) {
            RenderType type = transparent ? RenderTypes.entityTranslucentCullItemTarget(TEXTURE)
                    : RenderTypes.entityCutoutCull(TEXTURE);
            int color = ARGB.multiply(transparent ? 654311423 : -1, getModelTint(state));
            model.submitBottle(poses, collector, type, state, light, overlay, color);
        }
        // Custom geometry has no automatic model outline pass. Preserve standard glowing explicitly.
        if (state.appearsGlowing()) {
            model.submitBottle(poses, collector, RenderTypes.outline(TEXTURE), state,
                    light, overlay, state.outlineColor);
        }
    }
    @Override public GreenBottleRenderState createRenderState() { return new GreenBottleRenderState(); }
    @Override public Identifier getTextureLocation(GreenBottleRenderState state) { return TEXTURE; }
    @Override public void extractRenderState(GreenBottle bottle, GreenBottleRenderState state, float partial) {
        super.extractRenderState(bottle, state, partial);
        state.airborne = !bottle.onGround();
    }
}
