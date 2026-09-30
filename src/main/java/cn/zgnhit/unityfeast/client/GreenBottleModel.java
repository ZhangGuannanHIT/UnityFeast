package cn.zgnhit.unityfeast.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** A closed, lathed glass bottle, submitted as real geometry instead of modified cuboids. */
public final class GreenBottleModel extends EntityModel<GreenBottleRenderState> {
    private static final int SIDES = 16;
    private static final int CROWN_SIDES = 32;
    private static final List<Face> MESH = createMesh();

    public GreenBottleModel() {
        // MobRenderer retains facing, death, nameplate, leash and shadow handling.
        super(new ModelPart(List.of(), Map.of()));
    }

    public void submitBottle(PoseStack poses, SubmitNodeCollector collector, RenderType type,
            GreenBottleRenderState state, int light, int overlay, int color) {
        float motion = Math.min(.65F, state.walkAnimationSpeed);
        float step = Mth.sin(state.walkAnimationPos * 1.8F);
        poses.pushPose();
        poses.translate(0, (24 - Math.abs(step) * motion * .3F) / 16F, 0);
        poses.mulPose(Axis.ZP.rotation(step * motion * .12F));
        poses.mulPose(Axis.XP.rotation(state.airborne ? -.12F : -motion * .07F));
        // The collector snapshots the pose. Capture primitives and the shared immutable mesh,
        // never a live entity or mutable render state, in this deferred callback.
        collector.submitCustomGeometry(poses, type, (pose, buffer) -> {
            for (Face face : MESH) face.render(pose, buffer, light, overlay, color);
        });
        poses.popPose();
    }

    private static List<Face> createMesh() {
        var faces = new ArrayList<Face>();
        // Height/radius in model pixels: 24 pixels = 1.5 blocks. Rounded heel,
        // cylindrical body, curved shoulder, long narrow neck and molded mouth.
        float[][] glass = {{0,3.05F},{.25F,3.45F},{.8F,3.65F},{1.5F,3.7F},
                {12.6F,3.7F},{13.5F,3.6F},{14.4F,3.3F},{15.3F,2.75F},
                {16.2F,2.1F},{17.1F,1.6F},{18,1.4F},{21.8F,1.3F},
                {22.4F,1.3F},{22.55F,1.55F},{23.2F,1.55F},{23.35F,1.42F}};
        for (int ring = 0; ring < glass.length - 1; ring++) {
            for (int side = 0; side < SIDES; side++) {
                float h0 = glass[ring][0], h1 = glass[ring + 1][0];
                float r0 = glass[ring][1], r1 = glass[ring + 1][1];
                double a = side * Math.PI * 2 / SIDES, b = (side + 1) * Math.PI * 2 / SIDES;
                float u0 = side / (float) SIDES, u1 = (side + 1F) / SIDES;
                faces.add(face(vertex(a,r0,h0,u0,glassV(h0)), vertex(b,r0,h0,u1,glassV(h0)),
                        vertex(b,r1,h1,u1,glassV(h1)), vertex(a,r1,h1,u0,glassV(h1))));
            }
        }
        // Real alternating crown ridges, not a square cap with painted-on serrations.
        float[][] crown = {{23.15F,1.7F,.15F},{23.65F,1.65F,.08F},{23.9F,1.52F,0},{24,1.42F,0}};
        for (int ring = 0; ring < crown.length - 1; ring++) {
            for (int side = 0; side < CROWN_SIDES; side++) {
                int next = side + 1;
                double a = side * Math.PI * 2 / CROWN_SIDES, b = next * Math.PI * 2 / CROWN_SIDES;
                float[] lo = crown[ring], hi = crown[ring + 1];
                float u0 = side / (float) CROWN_SIDES, u1 = next / (float) CROWN_SIDES;
                faces.add(face(vertex(a,ridge(lo,side),lo[0],u0,metalV(lo[0])),
                        vertex(b,ridge(lo,next),lo[0],u1,metalV(lo[0])),
                        vertex(b,ridge(hi,next),hi[0],u1,metalV(hi[0])),
                        vertex(a,ridge(hi,side),hi[0],u0,metalV(hi[0]))));
            }
        }
        for (int side = 0; side < CROWN_SIDES; side++) {
            double a = side * Math.PI * 2 / CROWN_SIDES, b = (side + 1) * Math.PI * 2 / CROWN_SIDES;
            // Close the overhanging crown skirt from below; the inner edge overlaps the glass lip.
            float bottomV = 90F / 128;
            faces.add(face(vertex(b,ridge(crown[0],side+1),23.15F,(side+1F)/CROWN_SIDES,bottomV),
                    vertex(a,ridge(crown[0],side),23.15F,side/(float)CROWN_SIDES,bottomV),
                    vertex(a,1.48F,23.15F,side/(float)CROWN_SIDES,bottomV),
                    vertex(b,1.48F,23.15F,(side+1F)/CROWN_SIDES,bottomV)));
            Vertex center = new Vertex(0,-24F/16,0,104F/128,108F/128);
            faces.add(face(lid(a),lid(b),center,center));
        }
        for (int side = 0; side < SIDES; side++) {
            double a = side * Math.PI * 2 / SIDES, b = (side + 1) * Math.PI * 2 / SIDES;
            Vertex center = new Vertex(0,0,0,24F/128,108F/128);
            faces.add(face(vertex(b,3.05F,0,24F/128,108F/128),
                    vertex(a,3.05F,0,24F/128,108F/128),center,center));
        }
        return List.copyOf(faces);
    }

    private static float glassV(float height) { return (2 + (23.35F - height) * 3.18F) / 128; }
    private static float metalV(float height) { return (80 + (24 - height) * 13) / 128; }
    private static float ridge(float[] ring, int side) { return ring[1] + (side % 2 == 0 ? ring[2] : 0); }
    private static Vertex vertex(double angle, float radius, float height, float u, float v) {
        return new Vertex((float)Math.cos(angle)*radius/16, -height/16,
                (float)Math.sin(angle)*radius/16, u, v);
    }
    private static Vertex lid(double angle) {
        return vertex(angle,1.42F,24,(104+(float)Math.cos(angle)*14)/128,
                (108+(float)Math.sin(angle)*14)/128);
    }
    private static Face face(Vertex a, Vertex b, Vertex c, Vertex d) {
        Vector3f normal = new Vector3f(b.x-a.x,b.y-a.y,b.z-a.z)
                .cross(c.x-a.x,c.y-a.y,c.z-a.z).normalize();
        return new Face(a,b,c,d,normal.x,normal.y,normal.z);
    }
    private record Vertex(float x, float y, float z, float u, float v) {
        void render(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color,
                float nx, float ny, float nz) {
            buffer.addVertex(pose,x,y,z).setColor(color).setUv(u,v).setOverlay(overlay)
                    .setLight(light).setNormal(pose,nx,ny,nz);
        }
    }
    private record Face(Vertex a, Vertex b, Vertex c, Vertex d, float nx, float ny, float nz) {
        void render(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
            a.render(pose,buffer,light,overlay,color,nx,ny,nz);
            b.render(pose,buffer,light,overlay,color,nx,ny,nz);
            c.render(pose,buffer,light,overlay,color,nx,ny,nz);
            d.render(pose,buffer,light,overlay,color,nx,ny,nz);
        }
    }
}
