package com.example.makan.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.Minecraft;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "makan", value = Dist.CLIENT)
public class Grimoire3DRenderer {

    private static boolean active = false;
    private static float rotation = 0f;
    private static int ticks = 0;

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("makan", "textures/gui/grimoire.png");

    public static void showGrimoire() {
        active = true;
        rotation = 0f;
        ticks = 0;
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!active) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ticks++;
        rotation = Mth.lerp(0.1f, rotation, 90f);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        // Align to camera
        poseStack.mulPose(mc.getEntityRenderDispatcher().camera.rotation());

        // Move forward slightly
        poseStack.translate(0, -0.25, 0.5);

        // Tilt forward like a map
        poseStack.mulPose(Axis.XP.rotationDegrees(rotation));

        // Scale
        poseStack.scale(0.7f, 0.7f, 0.7f);

        // Render cuboid
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));

        float w = 1f, h = 1f, d = 0.1f;

        // FRONT (textured)
        quad(vc, poseStack,
            -w/2, 0, d/2, 0,0,
            w/2, 0, d/2, 1,0,
            w/2, h, d/2, 1,1,
            -w/2, h, d/2, 0,1,
            255,255,255,255,
            0,0,1);
        // FRONT (textured, extended for cover)
        // float coverExtension = 0.05f; // how much the cover sticks out
        // quad(vc, poseStack,
        //     -w/2 - coverExtension, 0, d/2, 0,0,
        //     w/2 + coverExtension, 0, d/2, 1,0,
        //     w/2 + coverExtension, h, d/2, 1,1,
        //     -w/2 - coverExtension, h, d/2, 0,1,
        //     255,255,255,255,
        //     0,0,1);

        // BACK (dimmed)
        // quad(vc, poseStack,
        //     -w/2, 0, -d/2, 0,0,
        //     w/2, 0, -d/2, 1,0,
        //     w/2, h, -d/2, 1,1,
        //     -w/2, h, -d/2, 0,1,
        //     200,200,200,255,
        //     0,0,-1);
        // BACK (curved)
        //renderCurvedBack(vc, poseStack, (float) 0.5*w, h, 3*d, 0.1f);

        // BACK (two curved halves)
        
        // left page
        poseStack.pushPose();
        poseStack.translate(-w/4f, 0, 0);
        renderCurvedBack(vc, poseStack, 0.5f * w, h, (float) 1.51*d, 0f, -0.03f); // small hump
        poseStack.popPose();

        // right page
        poseStack.pushPose();
        poseStack.translate(+w/4f, 0, 0);
        renderCurvedBack(vc, poseStack, 0.5f * w, h, (float) 1.51*d, 0f, -0.03f); // mirrored
        poseStack.popPose();



        // LEFT side
        quad(vc, poseStack,
            -w/2, 0, -d/2, 0,0,
            -w/2, 0,  d/2, 1,0,
            -w/2, h,  d/2, 1,1,
            -w/2, h, -d/2, 0,1,
            220,220,220,255,
            -1,0,0);

        // RIGHT side
        quad(vc, poseStack,
            w/2, 0, -d/2, 0,0,
            w/2, 0,  d/2, 1,0,
            w/2, h,  d/2, 1,1,
            w/2, h, -d/2, 0,1,
            220,220,220,255,
            1,0,0);

        // TOP (pages edge)
        quad(vc, poseStack,
            -w/2, h, -d/2, 0,0,
            -w/2, h,  d/2, 1,0,
            w/2, h,  d/2, 1,1,
            w/2, h, -d/2, 0,1,
            245,245,245,255,
            0,1,0);


        // BOTTOM (darker edge)
        // quad(vc, poseStack,
        //     -w/2, 0, -d/2, 0,0,
        //     -w/2, 0,  d/2, 1,0,
        //     w/2, 0,  d/2, 1,1,
        //     w/2, 0, -d/2, 0,1,
        //     180,180,180,255,
        //     0,-1,0);

        // left half
        poseStack.pushPose();
        poseStack.translate(-w/4f, 0, 0);
        renderCurvedBottom(vc, poseStack, 0.5f * w, (float)1.51*d, -0.03f);
        poseStack.popPose();

        // right half
        poseStack.pushPose();
        poseStack.translate(+w/4f, 0, 0);
        renderCurvedBottom(vc, poseStack, 0.5f * w, (float)1.51*d, -0.03f);
        poseStack.popPose();

        buffer.endBatch();
        poseStack.popPose();

        // if (ticks > 1000) active = false;
        // GrimoireDrawingScreen screen = new GrimoireDrawingScreen();
        // if (mc.screen == null) {
        //     mc.setScreen(new GrimoireDrawingScreen());
        // }
        //active = false;
    }

    private static void quad(VertexConsumer vc, PoseStack pose, 
                         float x1, float y1, float z1, float u1, float v1,
                         float x2, float y2, float z2, float u2, float v2,
                         float x3, float y3, float z3, float u3, float v3,
                         float x4, float y4, float z4, float u4, float v4,
                         int r, int g, int b, int a,
                         float nx, float ny, float nz) {
        vc.vertex(pose.last().pose(), x1, y1, z1).color(r,g,b,a)
        .uv(u1,v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(nx,ny,nz).endVertex();
        vc.vertex(pose.last().pose(), x2, y2, z2).color(r,g,b,a)
        .uv(u2,v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(nx,ny,nz).endVertex();
        vc.vertex(pose.last().pose(), x3, y3, z3).color(r,g,b,a)
        .uv(u3,v3).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(nx,ny,nz).endVertex();
        vc.vertex(pose.last().pose(), x4, y4, z4).color(r,g,b,a)
        .uv(u4,v4).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(nx,ny,nz).endVertex();
    }

    private static void renderCurvedBack(VertexConsumer vc, PoseStack poseStack,
                                     float w, float h, float d,
                                     float curveDepth,
                                     float curveHeight) {
        int xSeg = 64;
        int ySeg = 1;
        float stepX = w / xSeg;
        float stepY = h / ySeg;

        for (int i = 0; i < xSeg; i++) {
            float x1 = -w/2 + i * stepX;
            float x2 = x1 + stepX;

            // map across [-π, π]
            float t1 = (x1 / (w/2)) * (float)Math.PI;
            float t2 = (x2 / (w/2)) * (float)Math.PI;

            // push inward (negative Z side) with adjustable height
            float zOffset1 = -curveDepth + curveHeight * (float)Math.cos(t1);
            float zOffset2 = -curveDepth + curveHeight * (float)Math.cos(t2);

            for (int j = 0; j < ySeg; j++) {
                float y1 = j * stepY;
                float y2 = y1 + stepY;

                float v1 = (float) j / ySeg;
                float v2 = (float)(j+1) / ySeg;
                float u1 = (float) i / xSeg;
                float u2 = (float)(i+1) / xSeg;

                quad(vc, poseStack,
                    x1, y1, -d/2 + zOffset1, u1, v1,
                    x1, y2, -d/2 + zOffset1, u1, v2,
                    x2, y2, -d/2 + zOffset2, u2, v2,
                    x2, y1, -d/2 + zOffset2, u2, v1,
                    200, 200, 200, 255,
                    0, 0, -1
                );
            }
        }
    }

    private static void renderCurvedBottom(VertexConsumer vc, PoseStack poseStack,
                                       float w, float d, float curveHeight) {
        int xSeg = 64;
        float stepX = w / xSeg;

        for (int i = 0; i < xSeg; i++) {
            float x1 = -w/2 + i * stepX;
            float x2 = x1 + stepX;

            float t1 = (x1 / (w/2)) * (float)Math.PI;
            float t2 = (x2 / (w/2)) * (float)Math.PI;

            float zOffset1 = -0.0f + curveHeight * (float)Math.cos(t1);
            float zOffset2 = -0.0f + curveHeight * (float)Math.cos(t2);

            float u1 = (float) i / xSeg;
            float u2 = (float)(i+1) / xSeg;

            // Draw strip at bottom (y=0 plane)
            quad(vc, poseStack,
                x1, 0, -d/2 + zOffset1, u1, 0,
                x1, 0,  d/2,            u1, 1,
                x2, 0,  d/2,            u2, 1,
                x2, 0, -d/2 + zOffset2, u2, 0,
                180,180,180,255,
                0,-1,0
            );
        }
    }

    public static void setInactive() {
        active = false;
    }

    public static boolean isActive() {
        return active;
    }
}
