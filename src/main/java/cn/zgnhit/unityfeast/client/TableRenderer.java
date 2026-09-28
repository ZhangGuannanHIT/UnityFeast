package cn.zgnhit.unityfeast.client;

import cn.zgnhit.unityfeast.UnityFeastMod;
import cn.zgnhit.unityfeast.block.TableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

public final class TableRenderer implements BlockEntityRenderer<TableBlockEntity,TableRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState[] fish={new ItemStackRenderState(),new ItemStackRenderState(),new ItemStackRenderState(),new ItemStackRenderState()};
    }
    private final ItemModelResolver resolver;
    public TableRenderer(BlockEntityRendererProvider.Context context) { resolver=context.itemModelResolver(); }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(TableBlockEntity table,State state,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(table,state,partial,camera,breaking);
        for(int i=0;i<4;i++) {
            var stack=table.item(i);
            resolver.updateForTopItem(state.fish[i],stack.is(UnityFeastMod.DUMPLING.get())?ItemStack.EMPTY:stack,ItemDisplayContext.FIXED,table.getLevel(),null,i);
        }
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {
        for(int i=0;i<4;i++) if(!state.fish[i].isEmpty()) {
            pose.pushPose(); pose.translate(i==0||i==3?.27:.73,.83,i<2?.27:.73);
            pose.mulPose(Axis.XP.rotationDegrees(90)); pose.scale(.38F,.38F,.38F);
            state.fish[i].submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0); pose.popPose();
        }
    }
}
