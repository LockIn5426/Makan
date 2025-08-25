package com.example.makan.telekinesis;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWScrollCallbackI;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TelekinesisClientHandler {

    private static double scrollAmount = 0.0;
    public static float holdDistance = 4.0f;

    private static boolean callbackSet = false;
    private static long windowHandle = 0L;
    private static GLFWScrollCallbackI previousScrollCallback;

    public static boolean selectingRadius = false;
    public static int selectionRadius = 3;
    private static final int MIN_SELECTION_RADIUS = 1;
    private static final int MAX_SELECTION_RADIUS = 10;

    public static BlockPos currentTargetPos = null; // Raytraced target block
    private static boolean targetLocked = false;
    private static int overlayTicks = 0;

    // Client-side mirror of selected mobs (by entityId)
    public static final Set<Integer> selectedMobIdsClient = new HashSet<>();

    private static final GLFWScrollCallbackI scrollCallback = (window, xoffset, yoffset) -> {
        if (selectingRadius) {
            if (yoffset > 0) selectionRadius++;
            else if (yoffset < 0) selectionRadius--;
            if (selectionRadius < MIN_SELECTION_RADIUS) selectionRadius = MIN_SELECTION_RADIUS;
            if (selectionRadius > MAX_SELECTION_RADIUS) selectionRadius = MAX_SELECTION_RADIUS;
            overlayTicks = 20;
        } else if (TelekinesisHandler.heldEntity != null
                || TelekinesisHandler.heldClone != null
                || !TelekinesisHandler.heldMobs.isEmpty()) {
            scrollAmount += yoffset;
        } else if (previousScrollCallback != null) {
            previousScrollCallback.invoke(window, xoffset, yoffset);
        }
    };

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // Setup scroll callback
        Window window = mc.getWindow();
        long handle = window.getWindow();
        if (!callbackSet) {
            windowHandle = handle;
            previousScrollCallback = GLFW.glfwSetScrollCallback(windowHandle, scrollCallback);
            callbackSet = true;
        }

        // Adjust hold distance with scroll
        if ((TelekinesisHandler.heldEntity != null
                || TelekinesisHandler.heldClone != null
                || !TelekinesisHandler.heldMobs.isEmpty()) && scrollAmount != 0) {
            holdDistance += scrollAmount * 0.5f;
            if (holdDistance < 2.0f) holdDistance = 2.0f;
            if (holdDistance > 100.0f) holdDistance = 100.0f;
            scrollAmount = 0;
        }

        // Selection radius mode for blocks
        if (selectingRadius) {
            if (!targetLocked) {
                Vec3 eyePos = mc.player.getEyePosition();
                Vec3 lookVec = mc.player.getViewVector(1.0f).scale(100);
                BlockHitResult hit = mc.level.clip(new net.minecraft.world.level.ClipContext(
                        eyePos,
                        eyePos.add(lookVec),
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        mc.player
                ));

                if (hit.getType() == HitResult.Type.BLOCK) {
                    currentTargetPos = hit.getBlockPos();
                    targetLocked = true;
                }
            }

            if (overlayTicks > 0) {
                overlayTicks--;
                mc.gui.setOverlayMessage(Component.literal("Radius: " + selectionRadius + " — right-click to pick up"), false);
            }
        } else {
            currentTargetPos = null;
            targetLocked = false;
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 camPos = cam.getPosition();

        // Draw block selection box
        if (selectingRadius && currentTargetPos != null) {
            int r = selectionRadius;
            var c = currentTargetPos;

            double minX = c.getX() - r;
            double minY = c.getY() - r;
            double minZ = c.getZ() - r;
            double maxX = c.getX() + r + 1;
            double maxY = c.getY() + r + 1;
            double maxZ = c.getZ() + r + 1;

            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            pose.translate(-camPos.x, -camPos.y, -camPos.z);

            MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
            LevelRenderer.renderLineBox(
                    pose,
                    buffer.getBuffer(RenderType.lines()),
                    minX, minY, minZ, maxX, maxY, maxZ,
                    1.0F, 1.0F, 1.0F, 1.0F
            );

            pose.popPose();
            buffer.endBatch();
        }

        // Draw white bounding boxes around selected mobs
        if (!selectedMobIdsClient.isEmpty()) {
            PoseStack pose = event.getPoseStack();
            MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

            pose.pushPose();
            pose.translate(-camPos.x, -camPos.y, -camPos.z);

            for (LivingEntity mob : mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(128))) {
                if (!mob.isAlive()) continue;
                if (!selectedMobIdsClient.contains(mob.getId())) continue;

                var box = mob.getBoundingBox().inflate(0.02); // slight padding
                LevelRenderer.renderLineBox(
                        pose,
                        buffer.getBuffer(RenderType.lines()),
                        box.minX, box.minY, box.minZ,
                        box.maxX, box.maxY, box.maxZ,
                        1.0F, 1.0F, 1.0F, 1.0F
                );
            }

            pose.popPose();
            buffer.endBatch();
        }
    }
}
