package cn.zgnhit.unityfeast.client;

import cn.zgnhit.unityfeast.block.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

public final class CuttingBoardRenderer implements BlockEntityRenderer<CuttingBoardBlockEntity,CuttingBoardRenderer.State>{
    public static final class State extends BlockEntityRenderState {final ItemStackRenderState item=new ItemStackRenderState();float angle;}
    private final ItemModelResolver resolver;
    public CuttingBoardRenderer(BlockEntityRendererProvider.Context c){resolver=c.itemModelResolver();}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(CuttingBoardBlockEntity be,State s,float t,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay breaking){
        BlockEntityRenderer.super.extractRenderState(be,s,t,camera,breaking);
        resolver.updateForTopItem(s.item,be.ingredient(),ItemDisplayContext.FIXED,be.getLevel(),null,0);
        s.angle=be.getBlockState().getValue(CuttingBoardBlock.FACING).toYRot();
    }
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        if(s.item.isEmpty())return;
        pose.pushPose();pose.translate(.5,.145,.5);pose.mulPose(Axis.YP.rotationDegrees(-s.angle));
        pose.mulPose(Axis.XP.rotationDegrees(90));pose.scale(.65F,.65F,.65F);
        s.item.submit(pose,collector,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
    }
}
