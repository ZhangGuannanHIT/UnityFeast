package cn.zgnhit.unityfeast.client;

import cn.zgnhit.unityfeast.UnityFeastMod;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Original 16-pixel nose-to-tail silhouette. Body is only six pixels long. */
public final class RatModel extends EntityModel<RatRenderState> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(UnityFeastMod.id("rat"),"main");
    private final ModelPart head,tail,tail2,tail3;
    private final ModelPart[] legs=new ModelPart[4];
    public RatModel(ModelPart root) {
        super(root);head=root.getChild("head");tail=root.getChild("tail");tail2=tail.getChild("tail2");tail3=tail2.getChild("tail3");
        for(int i=0;i<4;i++) legs[i]=root.getChild("leg"+i);
    }
    public static LayerDefinition layer() {
        MeshDefinition mesh=new MeshDefinition();PartDefinition root=mesh.getRoot();
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-2, -3.5F,-3,4,3.5F,6),PartPose.offset(0,23,0));
        var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(20,0).addBox(-1.5F,-2.5F,-3,3,3,3),PartPose.offset(0,21.5F,-2));
        head.addOrReplaceChild("muzzle",CubeListBuilder.create().texOffs(34,0).addBox(-1,-.5F,-1.5F,2,1.5F,1.5F),PartPose.offset(0,-.3F,-3));
        head.addOrReplaceChild("ear_l",CubeListBuilder.create().texOffs(44,0).addBox(0,-1.8F,-.5F,1.5F,2,.5F),PartPose.offset(1, -2,-.5F));
        head.addOrReplaceChild("ear_r",CubeListBuilder.create().texOffs(44,0).addBox(-1.5F,-1.8F,-.5F,1.5F,2,.5F),PartPose.offset(-1,-2,-.5F));
        // Separate eye cubes share a tiny UV island; emissive texture contains this island only.
        head.addOrReplaceChild("eyes",CubeListBuilder.create().texOffs(56,0).addBox(-1.6F,-1.8F,-2.3F,.2F,.7F,.7F)
                .texOffs(56,0).addBox(1.4F,-1.8F,-2.3F,.2F,.7F,.7F),PartPose.ZERO);
        for(int i=0;i<4;i++) root.addOrReplaceChild("leg"+i,CubeListBuilder.create().texOffs(0,12).addBox(-.5F,0,-.6F,1,1.5F,1.8F),PartPose.offset(i%2==0?-1.5F:1.5F,22.5F,i<2?-2:2));
        var t=root.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(12,12).addBox(-.5F,-.5F,0,1,1,2.5F),PartPose.offset(0,22.3F,3));
        var t2=t.addOrReplaceChild("tail2",CubeListBuilder.create().texOffs(22,12).addBox(-.35F,-.35F,0,.7F,.7F,2.5F),PartPose.offset(0,0,2.5F));
        t2.addOrReplaceChild("tail3",CubeListBuilder.create().texOffs(32,12).addBox(-.2F,-.2F,0,.4F,.4F,1.5F),PartPose.offset(0,0,2.5F));
        return LayerDefinition.create(mesh,64,32);
    }
    @Override public void setupAnim(RatRenderState s) {
        super.setupAnim(s);head.yRot=s.yRot*Mth.DEG_TO_RAD;head.xRot=s.xRot*Mth.DEG_TO_RAD;
        for(int i=0;i<4;i++) legs[i].xRot=s.airborne?-.6F:Mth.cos(s.walkAnimationPos*1.8F+(i==0||i==3?0:Mth.PI))*s.walkAnimationSpeed*1.2F;
        float wave=Mth.sin(s.ageInTicks*.18F)*(.025F+s.walkAnimationSpeed*.18F);
        tail.yRot=wave;tail2.yRot=wave*1.2F;tail3.yRot=wave*1.4F;
        if(s.airborne){head.xRot-=.15F;tail.xRot=.18F;}
    }
}
