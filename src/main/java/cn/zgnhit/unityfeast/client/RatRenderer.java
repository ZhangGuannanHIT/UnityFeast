package cn.zgnhit.unityfeast.client;
import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.entity.Rat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;
public final class RatRenderer extends MobRenderer<Rat,RatRenderState,RatModel> {
    private static final Identifier TEXTURE=UnityFeastMod.id("textures/entity/rat.png");
    public RatRenderer(EntityRendererProvider.Context context) {
        super(context,new RatModel(context.bakeLayer(RatModel.LAYER)),.18F);
        addLayer(new EyesLayer<RatRenderState,RatModel>(this) {
            @Override public RenderType renderType(){return RenderTypes.eyes(UnityFeastMod.id("textures/entity/rat_eyes.png"));}
            @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,RatRenderState s,float y,float x){
                if(s.redEyes) super.submit(pose,collector,light,s,y,x);
            }
        });
    }
    @Override public RatRenderState createRenderState(){return new RatRenderState();}
    @Override public Identifier getTextureLocation(RatRenderState state){return TEXTURE;}
    @Override public void extractRenderState(Rat rat,RatRenderState s,float partial){super.extractRenderState(rat,s,partial);s.redEyes=rat.redEyes();s.airborne=!rat.onGround();}
}
