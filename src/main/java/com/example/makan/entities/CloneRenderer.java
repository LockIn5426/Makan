package com.example.makan.entities;

import com.example.makan.entities.CloneEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CloneRenderer extends HumanoidMobRenderer<CloneEntity, HumanoidModel<CloneEntity>> {
    private static final MinecraftSessionService sessionService = Minecraft.getInstance().getMinecraftSessionService();
    private final HumanoidModel<CloneEntity> slimModel;
    private final HumanoidModel<CloneEntity> defaultModel;

    public CloneRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.slimModel = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM));
        this.defaultModel = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER));
        this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(CloneEntity entity) {
        UUID ownerUUID = entity.getOwnerUUID();
        if (ownerUUID != null) {
            return getPlayerSkin(ownerUUID);
        }
        return DefaultPlayerSkin.getDefaultSkin();
    }

    private ResourceLocation getPlayerSkin(UUID uuid) {
        GameProfile profile = new GameProfile(uuid, null);
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = sessionService.getTextures(sessionService.fillProfileProperties(profile, false), false);
        if (textures.containsKey(MinecraftProfileTexture.Type.SKIN)) {
            MinecraftProfileTexture tex = textures.get(MinecraftProfileTexture.Type.SKIN);
            String skinUrl = tex.getUrl();
            String skinHash = tex.getHash();
            ResourceLocation location = new ResourceLocation("skins/" + skinHash);
            File skinDir = new File(FMLPaths.GAMEDIR.get().toFile(), "cached_skins");
            File skinFile = new File(skinDir, skinHash + ".png");
            if (!skinDir.exists()) skinDir.mkdirs();
            if (!skinFile.exists()) {
                try (InputStream in = new URL(skinUrl).openStream()) {
                    Files.copy(in, skinFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    return DefaultPlayerSkin.getDefaultSkin(uuid);
                }
            }
            Minecraft.getInstance().getTextureManager().register(location, new HttpTexture(skinFile, skinUrl, DefaultPlayerSkin.getDefaultSkin(uuid), false, null));
            return location;
        }
        return DefaultPlayerSkin.getDefaultSkin(uuid);
    }
}
