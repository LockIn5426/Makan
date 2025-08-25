package com.example.makan.telekinesis;

import com.example.makan.telekinesis.BlockEntry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

public class TelekinesisRenderer extends EntityRenderer<TelekinesisEntity> {
    private final BlockRenderDispatcher dispatcher;

    public TelekinesisRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.dispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(TelekinesisEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Minecraft.getInstance().execute(() -> {
        //     Minecraft.getInstance().player.sendSystemMessage(
        //         Component.literal("Rendering " + entity.getCarriedBlocks().size() + " blocks"));
        // });
        //int light = 15728880;
        for (BlockEntry entry : entity.getCarriedBlocks()) {
            BlockState block = entry.state;
            poseStack.pushPose();
            // Translate by relative block offsets
            poseStack.translate(entry.dx, entry.dy, entry.dz);
            dispatcher.renderSingleBlock(block, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.solid());
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public net.minecraft.resources.ResourceLocation getTextureLocation(TelekinesisEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
